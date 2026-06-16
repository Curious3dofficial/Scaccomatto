package com.scaccomatto.account;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public final class AccountServer {
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{3,24}");
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Duration SESSION_DURATION = Duration.ofHours(24);
    private static final Duration USERNAME_OTP_DURATION = Duration.ofMinutes(10);
    private static final Duration USERNAME_OTP_COOLDOWN = Duration.ofSeconds(60);
    private static final int USERNAME_OTP_MAX_ATTEMPTS = 5;
    private static final int MAX_BODY_BYTES = 16 * 1024;

    private static final int AUTH_RATE_LIMIT_ATTEMPTS = 5;
    private static final Duration AUTH_RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    private static final ConcurrentMap<String, AttemptHistory> authAttempts = new ConcurrentHashMap<>();

    private static final Path ACCESS_LOG_FILE = Path.of(env("ACCOUNT_LOG_FILE", "server/data/access.log"));
    private static final DateTimeFormatter ACCESS_LOG_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ACCESS_LOG_ENTRY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Object ACCESS_LOG_LOCK = new Object();
    private static final String REQUEST_START_ATTRIBUTE = "request_start_nanos";

    private final AccountRepository repository;
    private final EmailSender emailSender;
    private final EloSystem eloSystem;

    private static boolean isRateLimited(HttpExchange exchange, String action) {
        String key = rateLimitKey(exchange, action);
        AttemptHistory history = authAttempts.get(key);
        if (history == null) return false;

        boolean exceeded = history.isExceeded(Instant.now());
        if (history.isEmpty()) {
            authAttempts.remove(key, history);
        }
        return exceeded;
    }

    private static void registerAttempt(HttpExchange exchange, String action) {
        String key = rateLimitKey(exchange, action);
        authAttempts.computeIfAbsent(key, ignored -> new AttemptHistory()).record(Instant.now());
    }

    private static String rateLimitKey(HttpExchange exchange, String action) {
        InetSocketAddress remote = exchange.getRemoteAddress();
        String ip = remote == null || remote.getAddress() == null
                ? "unknown"
                : remote.getAddress().getHostAddress();
        return ip + ":" + action;
    }

    private static final class AttemptHistory {
        private final Deque<Instant> attempts = new java.util.ArrayDeque<>();

        synchronized boolean isExceeded(Instant now) {
            prune(now);
            return attempts.size() >= AUTH_RATE_LIMIT_ATTEMPTS;
        }

        synchronized boolean isEmpty() {
            prune(Instant.now());
            return attempts.isEmpty();
        }

        synchronized void record(Instant now) {
            prune(now);
            attempts.addLast(now);
        }

        private void prune(Instant now) {
            Instant boundary = now.minus(AUTH_RATE_LIMIT_WINDOW);
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(boundary)) {
                attempts.removeFirst();
            }
        }
    }

    private AccountServer(
            AccountRepository repository,
            EmailSender emailSender,
            EloSystem eloSystem) {
        this.repository = repository;
        this.emailSender = emailSender;
        this.eloSystem = eloSystem;
    }

    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(env("ACCOUNT_PORT", "8080"));
            String databaseUrl = env(
                    "ACCOUNT_DB_URL",
                    "jdbc:h2:file:./server/data/scaccomatto;AUTO_SERVER=FALSE");
            Path migration = Path.of(env(
                    "ACCOUNT_MIGRATION",
                    "server/db/migration"));
            Path eloRules = Path.of(env(
                    "ACCOUNT_ELO_RULES",
                    "server/data/scaccomatto_elo_system.json"));

            Class.forName("org.h2.Driver");
            Database database = new Database(databaseUrl);
            database.migrate(migration);
            AccountRepository repository = new AccountRepository(database);
            repository.deleteExpiredSessions();

            AccountServer application = new AccountServer(
                    repository,
                    createEmailSender(),
                    EloSystem.load(eloRules));
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/api/health", application.logRequests(application::health));
        server.createContext("/api/auth/register", application.logRequests(application::register));
        server.createContext("/api/auth/login", application.logRequests(application::login));
        server.createContext("/api/auth/remember", application.logRequests(application::remember));
        server.createContext("/api/auth/remember/delete", application.logRequests(application::deleteRememberToken));
        server.createContext("/api/auth/sessions/all/delete", application.logRequests(application::deleteAllRememberTokens));
        server.createContext("/api/auth/logout", application.logRequests(application::logout));
        server.createContext("/api/account/me", application.logRequests(application::profile));
        server.createContext("/api/account/settings", application.logRequests(application::settings));
        server.createContext("/api/account/game-result", application.logRequests(application::gameResult));
        server.createContext(
                "/api/account/username/request",
                application.logRequests(application::requestUsernameChange));
        server.createContext(
                "/api/account/username/verify",
                application.logRequests(application::verifyUsernameChange));
        server.setExecutor(Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "account-api");
            thread.setDaemon(true);
            return thread;
        }));
        server.start();
        System.out.println("Scaccomatto account API listening on http://127.0.0.1:" + port);
        System.out.println("Database: " + databaseUrl);
        } catch (Throwable exception) {
            System.err.println("Failed to start Scaccomatto account API: " + exception.getMessage());
            exception.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private HttpHandler logRequests(ExchangeHandler handler) {
        return exchange -> {
            exchange.setAttribute(REQUEST_START_ATTRIBUTE, System.nanoTime());
            try {
                handler.handle(exchange);
            } catch (IOException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                serverError(exchange, exception);
            }
        };
    }

    private static Path datedAccessLogPath() {
        String date = LocalDate.now().format(ACCESS_LOG_DATE_FORMAT);
        String fileName = ACCESS_LOG_FILE.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex > 0) {
            fileName = fileName.substring(0, extensionIndex)
                    + "-" + date
                    + fileName.substring(extensionIndex);
        } else {
            fileName = fileName + "-" + date;
        }
        return ACCESS_LOG_FILE.resolveSibling(fileName);
    }

    private static long requestDurationMillis(HttpExchange exchange) {
        Object value = exchange.getAttribute(REQUEST_START_ATTRIBUTE);
        if (value instanceof Long start) {
            return Math.max(0, (System.nanoTime() - start) / 1_000_000);
        }
        return 0;
    }

    private static void logRequest(HttpExchange exchange, int status, long durationMs) {
        String timestamp = LocalDateTime.now().format(ACCESS_LOG_ENTRY_FORMAT);
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        InetSocketAddress remote = exchange.getRemoteAddress();
        String ip = remote == null || remote.getAddress() == null
                ? "unknown"
                : remote.getAddress().getHostAddress();
        String line = String.format("[%s] %s %s ip=%s → %d (%dms)",
                timestamp, method, path, ip, status, durationMs);
        Path logFile = datedAccessLogPath();
        try {
            synchronized (ACCESS_LOG_LOCK) {
                Path parent = logFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                try (BufferedWriter writer = Files.newBufferedWriter(
                        logFile,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        } catch (IOException exception) {
            System.err.println("Failed to write access log: " + exception.getMessage());
        }
    }

    private void health(HttpExchange exchange) throws IOException {
        if (!method(exchange, "GET")) return;
        send(exchange, 200, Map.of("status", "ok"));
    }

    private void register(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        try {
            Map<String, String> body = readJson(exchange);
            String profileName = clean(body.get("profileName"));
            String username = clean(body.get("username"));
            String email = clean(body.get("email"));
            char[] password = password(body.get("password"));
            validateRegistration(profileName, username, email, password);

            AccountRepository.User user;
            try {
                user = repository.createUser(
                        username,
                        email,
                        Security.hashPassword(password),
                        profileName);
            } finally {
                java.util.Arrays.fill(password, '\0');
            }
            sendSession(exchange, 201, user);
        } catch (SQLIntegrityConstraintViolationException exception) {
            error(exchange, 409, "account_exists", "That username or email is already registered.");
        } catch (SQLException exception) {
            if ("23505".equals(exception.getSQLState())) {
                error(exchange, 409, "account_exists", "That username or email is already registered.");
            } else {
                serverError(exchange, exception);
            }
        } catch (ApiException exception) {
            error(exchange, exception.status, exception.code, exception.getMessage());
        } catch (RuntimeException exception) {
            error(exchange, 400, "invalid_json", "The request body is not valid JSON.");
        }
    }

    private void login(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        if (isRateLimited(exchange, "login")) {
            error(exchange, 429, "rate_limited", "Too many login attempts. Try again later.");
            return;
        }
        try {
            registerAttempt(exchange, "login");
            Map<String, String> body = readJson(exchange);
            String login = clean(body.get("login"));
            char[] password = password(body.get("password"));
            if (login.isEmpty() || password.length == 0) {
                throw new ApiException(400, "missing_fields", "Login and password are required.");
            }

            Optional<AccountRepository.User> candidate = repository.findByLogin(login);
            boolean valid;
            try {
                valid = candidate.isPresent()
                        && Security.verifyPassword(password, candidate.get().passwordHash());
            } finally {
                java.util.Arrays.fill(password, '\0');
            }
            if (!valid) {
                throw new ApiException(401, "invalid_credentials", "Incorrect username, email, or password.");
            }
            if (candidate.get().banned()) {
                throw new ApiException(403, "account_banned", "Your account has been banned.");
            }

            Map<String, Object> extras = new java.util.LinkedHashMap<>();
            if (Boolean.parseBoolean(body.get("remember"))) {
                String rememberToken = repository.createRememberToken(
                        candidate.get().id(),
                        Instant.now().plus(30, ChronoUnit.DAYS));
                extras.put("rememberToken", rememberToken);
            }
            sendSession(exchange, 200, candidate.orElseThrow(), extras);
        } catch (ApiException exception) {
            error(exchange, exception.status, exception.code, exception.getMessage());
        } catch (SQLException exception) {
            serverError(exchange, exception);
        } catch (RuntimeException exception) {
            error(exchange, 400, "invalid_json", "The request body is not valid JSON.");
        }
    }

    private void remember(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        if (isRateLimited(exchange, "remember")) {
            error(exchange, 429, "rate_limited", "Too many remember attempts. Try again later.");
            return;
        }
        try {
            registerAttempt(exchange, "remember");
            Map<String, String> payload = readJson(exchange);
            String rememberToken = payload.get("rememberToken");
            if (rememberToken == null || rememberToken.isBlank()) {
                throw new ApiException(400, "missing_remember_token", "A remember token is required.");
            }

            Optional<AccountRepository.RememberToken> token = repository.findRememberToken(rememberToken);
            if (token.isEmpty()) {
                throw new ApiException(401, "invalid_remember_token", "The remember token is invalid or expired.");
            }
            if (token.get().expiresAt().isBefore(Instant.now())) {
                repository.deleteRememberToken(rememberToken);
                throw new ApiException(401, "remember_token_expired", "The remember token has expired.");
            }

            UUID userId = token.get().userId();
            repository.deleteRememberToken(rememberToken);
            String newRememberToken = repository.createRememberToken(userId, Instant.now().plus(30, ChronoUnit.DAYS));
            AccountRepository.User user = repository.findById(userId).orElseThrow();
            if (user.banned()) {
                repository.deleteRememberToken(newRememberToken);
                throw new ApiException(403, "account_banned", "Your account has been banned.");
            }
            sendSession(exchange, 200, user, Map.of("rememberToken", newRememberToken));
        } catch (ApiException exception) {
            error(exchange, exception.status, exception.code, exception.getMessage());
        } catch (SQLException exception) {
            serverError(exchange, exception);
        } catch (RuntimeException exception) {
            error(exchange, 400, "invalid_json", "The request body is not valid JSON.");
        }
    }

    private void deleteRememberToken(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        try {
            Map<String, String> payload = readJson(exchange);
            String rememberToken = payload.get("rememberToken");
            if (rememberToken != null && !rememberToken.isBlank()) {
                repository.deleteRememberToken(rememberToken);
            }
            send(exchange, 200, Map.of("status", "ok"));
        } catch (IOException exception) {
            throw exception;
        } catch (ApiException exception) {
            error(exchange, exception.status, exception.code, exception.getMessage());
        } catch (SQLException exception) {
            serverError(exchange, exception);
        } catch (RuntimeException exception) {
            error(exchange, 400, "invalid_json", "The request body is not valid JSON.");
        }
    }

    private void deleteAllRememberTokens(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        String token = bearerToken(exchange);
        if (token == null) {
            error(exchange, 401, "unauthorized", "A bearer token is required.");
            return;
        }
        try {
            AccountRepository.User user = authenticatedUser(token);
            repository.deleteRememberTokensByUserId(user.id());
            send(exchange, 200, Map.of("status", "ok"));
        } catch (ApiException exception) {
            error(exchange, exception.status, exception.code, exception.getMessage());
        } catch (SQLException exception) {
            serverError(exchange, exception);
        } catch (RuntimeException exception) {
            error(exchange, 400, "invalid_json", "The request body is not valid JSON.");
        }
    }

    private void logout(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        String token = bearerToken(exchange);
        if (token == null) {
            error(exchange, 401, "unauthorized", "A bearer token is required.");
            return;
        }
        try {
            repository.deleteSession(token);
            send(exchange, 200, Map.of("status", "logged_out"));
        } catch (SQLException exception) {
            serverError(exchange, exception);
        }
    }

    private void profile(HttpExchange exchange) throws IOException {
        String token = bearerToken(exchange);
        if (token == null) {
            error(exchange, 401, "unauthorized", "A bearer token is required.");
            return;
        }
        try {
            Optional<AccountRepository.User> authenticated = repository.findBySessionToken(token);
            if (authenticated.isEmpty()) {
                error(exchange, 401, "session_expired", "Your session has expired. Please sign in again.");
                return;
            }
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 200, userJson(authenticated.get()));
                return;
            }
            if ("PATCH".equalsIgnoreCase(exchange.getRequestMethod())) {
                String profileName = clean(readJson(exchange).get("profileName"));
                validateProfileName(profileName);
                AccountRepository.User updated = repository.updateDisplayName(
                        authenticated.get().id(),
                        profileName);
                send(exchange, 200, userJson(updated));
                return;
            }
            allow(exchange, "GET, PATCH");
        } catch (ApiException exception) {
            error(exchange, exception.status, exception.code, exception.getMessage());
        } catch (SQLException exception) {
            serverError(exchange, exception);
        } catch (RuntimeException exception) {
            error(exchange, 400, "invalid_json", "The request body is not valid JSON.");
        }
    }

    private void settings(HttpExchange exchange) throws IOException {
        String token = bearerToken(exchange);
        if (token == null) {
            error(exchange, 401, "unauthorized", "A bearer token is required.");
            return;
        }
        try {
            AccountRepository.User user = authenticatedUser(token);
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String settingsJson = user.settingsJson();
                if (settingsJson == null || settingsJson.isBlank()) {
                    settingsJson = "{}";
                }
                send(exchange, 200, Json.parseObject(settingsJson));
                return;
            }
            if ("PATCH".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = readJson(exchange);
                Map<String, String> currentSettings = Json.parseObject(
                        user.settingsJson() == null || user.settingsJson().isBlank()
                                ? "{}"
                                : user.settingsJson());
                currentSettings.putAll(body);
                String updatedJson = Json.object(currentSettings);
                repository.updateSettings(user.id(), updatedJson);
                send(exchange, 200, currentSettings);
                return;
            }
            allow(exchange, "GET, PATCH");
        } catch (ApiException exception) {
            error(exchange, exception.status, exception.code, exception.getMessage());
        } catch (SQLException exception) {
            serverError(exchange, exception);
        } catch (RuntimeException exception) {
            error(exchange, 400, "invalid_json", "The request body is not valid JSON.");
        }
    }

    private void requestUsernameChange(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        String token = bearerToken(exchange);
        if (token == null) {
            error(exchange, 401, "unauthorized", "A bearer token is required.");
            return;
        }
        try {
            AccountRepository.User user = authenticatedUser(token);
            String newUsername = clean(readJson(exchange).get("newUsername"));
            validateUsername(newUsername);
            if (newUsername.equalsIgnoreCase(user.username())) {
                throw new ApiException(
                        400,
                        "username_unchanged",
                        "Enter a username different from your current username.");
            }
            if (repository.usernameExists(newUsername)) {
                throw new ApiException(409, "username_taken", "That username is already taken.");
            }

            Optional<AccountRepository.UsernameChangeRequest> existing =
                    repository.findUsernameChangeRequest(user.id());
            if (existing.isPresent()
                    && existing.get().createdAt().plus(USERNAME_OTP_COOLDOWN).isAfter(Instant.now())) {
                long retryAfter = Duration.between(
                        Instant.now(),
                        existing.get().createdAt().plus(USERNAME_OTP_COOLDOWN)).toSeconds() + 1;
                throw new ApiException(
                        429,
                        "otp_rate_limited",
                        "Wait " + retryAfter + " seconds before requesting another code.");
            }

            String code = Security.newOtp();
            char[] otp = code.toCharArray();
            String otpHash;
            try {
                otpHash = Security.hashPassword(otp);
            } finally {
                java.util.Arrays.fill(otp, '\0');
            }
            Instant expiresAt = Instant.now().plus(USERNAME_OTP_DURATION);
            repository.saveUsernameChangeRequest(user.id(), newUsername, otpHash, expiresAt);

            String delivery;
            try {
                delivery = emailSender.sendUsernameChangeCode(
                        user.email(),
                        user.username(),
                        newUsername,
                        code);
            } catch (Exception exception) {
                repository.deleteUsernameChangeRequest(user.id());
                throw new ApiException(
                        503,
                        "email_delivery_failed",
                        "The verification email could not be sent. Try again later.");
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "otp_sent");
            response.put("email", maskEmail(user.email()));
            response.put("expiresAt", expiresAt);
            response.put("delivery", delivery);
            send(exchange, 200, response);
        } catch (ApiException exception) {
            error(exchange, exception.status, exception.code, exception.getMessage());
        } catch (SQLException exception) {
            serverError(exchange, exception);
        } catch (RuntimeException exception) {
            error(exchange, 400, "invalid_json", "The request body is not valid JSON.");
        }
    }

    private void verifyUsernameChange(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        String token = bearerToken(exchange);
        if (token == null) {
            error(exchange, 401, "unauthorized", "A bearer token is required.");
            return;
        }
        try {
            AccountRepository.User user = authenticatedUser(token);
            String code = clean(readJson(exchange).get("code"));
            if (!code.matches("\\d{6}")) {
                throw new ApiException(400, "invalid_otp", "Enter the six-digit verification code.");
            }

            AccountRepository.UsernameChangeRequest request =
                    repository.findUsernameChangeRequest(user.id())
                            .orElseThrow(() -> new ApiException(
                                    404,
                                    "otp_not_requested",
                                    "Request a username verification code first."));
            if (!request.expiresAt().isAfter(Instant.now())) {
                repository.deleteUsernameChangeRequest(user.id());
                throw new ApiException(410, "otp_expired", "That verification code has expired.");
            }
            if (request.attempts() >= USERNAME_OTP_MAX_ATTEMPTS) {
                repository.deleteUsernameChangeRequest(user.id());
                throw new ApiException(
                        429,
                        "otp_attempts_exceeded",
                        "Too many incorrect attempts. Request a new code.");
            }

            char[] otp = code.toCharArray();
            boolean valid;
            try {
                valid = Security.verifyPassword(otp, request.otpHash());
            } finally {
                java.util.Arrays.fill(otp, '\0');
            }
            if (!valid) {
                repository.incrementUsernameChangeAttempts(user.id());
                throw new ApiException(400, "invalid_otp", "That verification code is incorrect.");
            }
            if (repository.usernameExists(request.newUsername())) {
                repository.deleteUsernameChangeRequest(user.id());
                throw new ApiException(
                        409,
                        "username_taken",
                        "That username was claimed before verification completed.");
            }

            AccountRepository.User updated = repository.applyUsernameChange(
                    user.id(),
                    request.newUsername());
            send(exchange, 200, userJson(updated));
        } catch (ApiException exception) {
            error(exchange, exception.status, exception.code, exception.getMessage());
        } catch (SQLException exception) {
            if ("23505".equals(exception.getSQLState())) {
                error(exchange, 409, "username_taken", "That username is already taken.");
            } else {
                serverError(exchange, exception);
            }
        } catch (RuntimeException exception) {
            error(exchange, 400, "invalid_json", "The request body is not valid JSON.");
        }
    }

    private void gameResult(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        String token = bearerToken(exchange);
        if (token == null) {
            error(exchange, 401, "unauthorized", "A bearer token is required.");
            return;
        }
        try {
            AccountRepository.User user = authenticatedUser(token);
            String result = normalizeGameResult(clean(readJson(exchange).get("result")));
            EloSystem.RatingChange change = eloSystem.apply(user.rating(), result);
            AccountRepository.User updated = repository.applyGameResult(
                    user.id(),
                    change.newRating(),
                    result);

            Map<String, Object> response = userJson(updated);
            response.put("result", result);
            response.put("previousRating", change.previousRating());
            response.put("newRating", change.newRating());
            response.put("ratingDelta", change.ratingDelta());
            response.put("previousRank", change.previousRank());
            response.put("newRank", change.newRank());
            send(exchange, 200, response);
        } catch (ApiException exception) {
            error(exchange, exception.status, exception.code, exception.getMessage());
        } catch (SQLException exception) {
            serverError(exchange, exception);
        } catch (RuntimeException exception) {
            error(exchange, 400, "invalid_json", "The request body is not valid JSON.");
        }
    }

    private void sendSession(HttpExchange exchange, int status, AccountRepository.User user)
            throws SQLException, IOException {
        sendSession(exchange, status, user, Map.of());
    }

    private void sendSession(HttpExchange exchange, int status, AccountRepository.User user,
                             Map<String, ?> extraFields)
            throws SQLException, IOException {
        Instant expiresAt = Instant.now().plus(SESSION_DURATION);
        String token = repository.createSession(user.id(), expiresAt);
        Map<String, Object> response = new LinkedHashMap<>(userJson(user));
        response.putAll(extraFields);
        response.put("token", token);
        response.put("expiresAt", expiresAt.toString());
        send(exchange, status, response);
    }

    private static Map<String, Object> userJson(AccountRepository.User user) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.id());
        response.put("username", user.username());
        response.put("email", user.email());
        response.put("profileName", user.displayName());
        response.put("rating", user.rating());
        response.put("wins", user.wins());
        response.put("losses", user.losses());
        response.put("draws", user.draws());
        response.put("createdAt", user.createdAt());
        return response;
    }

    private static void validateRegistration(
            String profileName,
            String username,
            String email,
            char[] password) throws ApiException {
        validateProfileName(profileName);
        validateUsername(username);
        if (!EMAIL.matcher(email).matches() || email.length() > 254) {
            throw new ApiException(400, "invalid_email", "Enter a valid email address.");
        }
        if (password.length < 10 || password.length > 128) {
            throw new ApiException(
                    400,
                    "invalid_password",
                    "Password must contain 10 to 128 characters.");
        }
    }

    private static void validateProfileName(String profileName) throws ApiException {
        if (profileName.length() < 2 || profileName.length() > 40) {
            throw new ApiException(
                    400,
                    "invalid_profile_name",
                    "Profile Name must contain 2 to 40 characters.");
        }
    }

    private AccountRepository.User authenticatedUser(String token)
            throws SQLException, ApiException {
        return repository.findBySessionToken(token)
                .orElseThrow(() -> new ApiException(
                        401,
                        "session_expired",
                        "Your session has expired. Please sign in again."));
    }

    private static void validateUsername(String username) throws ApiException {
        if (!USERNAME.matcher(username).matches()) {
            throw new ApiException(
                    400,
                    "invalid_username",
                    "Username must be 3 to 24 characters using letters, numbers, or underscores.");
        }
    }

    private static String normalizeGameResult(String result) throws ApiException {
        String normalized = result.toLowerCase();
        if ("won".equals(normalized) || "win".equals(normalized)) return "win";
        if ("lost".equals(normalized) || "lose".equals(normalized)
                || "loss".equals(normalized)) return "loss";
        if ("drawn".equals(normalized) || "tie".equals(normalized)
                || "draw".equals(normalized)) return "draw";
        throw new ApiException(
                400,
                "invalid_game_result",
                "Game result must be win, loss, or draw.");
    }

    private static EmailSender createEmailSender() {
        String smtpHost = System.getenv("SMTP_HOST");
        if (smtpHost == null || smtpHost.isBlank()) {
            return new DevelopmentEmailSender(Path.of(env(
                    "ACCOUNT_OTP_OUTBOX",
                    "server/data/otp-outbox.log")));
        }
        return new SmtpEmailSender(
                smtpHost,
                Integer.parseInt(env("SMTP_PORT", "465")),
                env("SMTP_USERNAME", ""),
                env("SMTP_PASSWORD", ""),
                env("SMTP_FROM", env("SMTP_USERNAME", "")));
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***" + email.substring(Math.max(0, at));
        return email.substring(0, 1) + "***" + email.substring(at);
    }

    private static Map<String, String> readJson(HttpExchange exchange) throws IOException, ApiException {
        int declaredLength = 0;
        String contentLength = exchange.getRequestHeaders().getFirst("Content-Length");
        if (contentLength != null) {
            try {
                declaredLength = Integer.parseInt(contentLength);
            } catch (NumberFormatException ignored) {
            }
        }
        if (declaredLength > MAX_BODY_BYTES) {
            throw new ApiException(413, "body_too_large", "Request body is too large.");
        }
        byte[] body = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            throw new ApiException(413, "body_too_large", "Request body is too large.");
        }
        return Json.parseObject(new String(body, StandardCharsets.UTF_8));
    }

    private static boolean method(HttpExchange exchange, String expected) throws IOException {
        if (expected.equalsIgnoreCase(exchange.getRequestMethod())) return true;
        allow(exchange, expected);
        return false;
    }

    private static void allow(HttpExchange exchange, String methods) throws IOException {
        exchange.getResponseHeaders().set("Allow", methods);
        error(exchange, 405, "method_not_allowed", "This endpoint does not support that method.");
    }

    private static String bearerToken(HttpExchange exchange) {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        String token = authorization.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static void send(HttpExchange exchange, int status, Map<String, ?> response)
            throws IOException {
        byte[] payload = Json.object(response).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, payload.length);
        exchange.getResponseBody().write(payload);
        exchange.close();
        logRequest(exchange, status, requestDurationMillis(exchange));
    }

    private static void error(
            HttpExchange exchange,
            int status,
            String code,
            String message) throws IOException {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", code);
        response.put("message", message);
        send(exchange, status, response);
    }

    private static void serverError(HttpExchange exchange, Exception exception) throws IOException {
        exception.printStackTrace(System.err);
        error(exchange, 500, "server_error", "The account service encountered an error.");
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static char[] password(String value) {
        return value == null ? new char[0] : value.toCharArray();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

}
