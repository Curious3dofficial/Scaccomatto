import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AccountApiClient {
    public record Profile(
            String id,
            String username,
            String email,
            String profileName,
            int rating,
            int wins,
            int losses,
            int draws,
            String createdAt) {
    }

    public record Session(String token, String expiresAt, Profile profile, String rememberToken) {
    }

    public record OtpDelivery(String email, String expiresAt, String delivery) {
    }

    public record RatingUpdate(
            Profile profile,
            String result,
            int previousRating,
            int newRating,
            int ratingDelta,
            String previousRank,
            String newRank) {
    }

    public static final class ApiException extends Exception {
        private final int statusCode;

        ApiException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

    private final HttpClient client;
    private final String baseUrl;

    public AccountApiClient() {
        this(System.getProperty(
                "scaccomatto.accountApi",
                System.getenv().getOrDefault(
                        "SCACCOMATTO_ACCOUNT_API",
                        "http://127.0.0.1:8080")));
    }

    AccountApiClient(String baseUrl) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
    }

    public Session register(String profileName, String username, String email, char[] password)
            throws IOException, InterruptedException, ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("profileName", profileName);
        body.put("username", username);
        body.put("email", email);
        body.put("password", new String(password));
        return session(send("POST", "/api/auth/register", body, null));
    }

    public Session login(String login, char[] password)
            throws IOException, InterruptedException, ApiException {
        return login(login, password, false);
    }

    public Session login(String login, char[] password, boolean remember)
            throws IOException, InterruptedException, ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("login", login);
        body.put("password", new String(password));
        if (remember) {
            body.put("remember", true);
        }
        return session(send("POST", "/api/auth/login", body, null));
    }

    public Profile getProfile(String token)
            throws IOException, InterruptedException, ApiException {
        return profile(send("GET", "/api/account/me", null, token));
    }

    public Map<String, String> getSettings(String token)
            throws IOException, InterruptedException, ApiException {
        return send("GET", "/api/account/settings", null, token);
    }

    public Map<String, String> updateSettings(String token, Map<String, ?> settings)
            throws IOException, InterruptedException, ApiException {
        return send("PATCH", "/api/account/settings", settings, token);
    }

    public Profile updateProfileName(String token, String profileName)
            throws IOException, InterruptedException, ApiException {
        return profile(send(
                "PATCH",
                "/api/account/me",
                Map.of("profileName", profileName),
                token));
    }

    public OtpDelivery requestUsernameChange(String token, String newUsername)
            throws IOException, InterruptedException, ApiException {
        Map<String, String> values = send(
                "POST",
                "/api/account/username/request",
                Map.of("newUsername", newUsername),
                token);
        return new OtpDelivery(
                required(values, "email"),
                required(values, "expiresAt"),
                required(values, "delivery"));
    }

    public Profile verifyUsernameChange(String token, String code)
            throws IOException, InterruptedException, ApiException {
        return profile(send(
                "POST",
                "/api/account/username/verify",
                Map.of("code", code),
                token));
    }

    public RatingUpdate recordGameResult(String token, String result)
            throws IOException, InterruptedException, ApiException {
        Map<String, String> values = send(
                "POST",
                "/api/account/game-result",
                Map.of("result", result),
                token);
        return new RatingUpdate(
                profile(values),
                required(values, "result"),
                number(values, "previousRating"),
                number(values, "newRating"),
                number(values, "ratingDelta"),
                required(values, "previousRank"),
                required(values, "newRank"));
    }

    public void logout(String token) throws IOException, InterruptedException, ApiException {
        send("POST", "/api/auth/logout", Map.of(), token);
    }

    public Session remember(String rememberToken)
            throws IOException, InterruptedException, ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("rememberToken", rememberToken);
        return session(send("POST", "/api/auth/remember", body, null));
    }

    public void deleteRememberToken(String rememberToken)
            throws IOException, InterruptedException, ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("rememberToken", rememberToken);
        send("POST", "/api/auth/remember/delete", body, null);
    }

    private Map<String, String> send(
            String method,
            String path,
            Map<String, ?> body,
            String token) throws IOException, InterruptedException, ApiException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json");
        if (token != null && !token.isBlank()) {
            request.header("Authorization", "Bearer " + token);
        }
        if (body == null && "GET".equals(method)) {
            request.GET();
        } else {
            String json = AccountJson.object(body == null ? Map.of() : body);
            request.header("Content-Type", "application/json");
            request.method(method, HttpRequest.BodyPublishers.ofString(json));
        }

        HttpResponse<String> response;
        try {
            response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (ConnectException exception) {
            throw new IOException(
                    "The account server is offline. Start server/scripts/run.sh first.",
                    exception);
        }
        Map<String, String> values;
        try {
            values = AccountJson.parseObject(response.body());
        } catch (RuntimeException exception) {
            throw new IOException("The account server returned an invalid response.", exception);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String message = values.get("message");
            throw new ApiException(
                    response.statusCode(),
                    message == null ? "Account request failed." : message);
        }
        return values;
    }

    private static Session session(Map<String, String> values) throws IOException {
        String token = required(values, "token");
        return new Session(
                token,
                required(values, "expiresAt"),
                profile(values),
                values.get("rememberToken"));
    }

    private static Profile profile(Map<String, String> values) throws IOException {
        return new Profile(
                required(values, "id"),
                required(values, "username"),
                required(values, "email"),
                required(values, "profileName"),
                number(values, "rating"),
                number(values, "wins"),
                number(values, "losses"),
                number(values, "draws"),
                required(values, "createdAt"));
    }

    private static int number(Map<String, String> values, String key) throws IOException {
        try {
            return Integer.parseInt(required(values, key));
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid numeric account field: " + key, exception);
        }
    }

    private static String required(Map<String, String> values, String key) throws IOException {
        String value = values.get(key);
        if (value == null) throw new IOException("Missing account field: " + key);
        return value;
    }

    private static String stripTrailingSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result.isEmpty() ? "http://127.0.0.1:8080" : result;
    }
}
