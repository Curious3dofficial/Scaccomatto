# Scaccomatto Account Service

This is a separate local HTTP backend for the desktop game. The Swing client never
opens the database directly.

## Security

- Passwords are salted with 16 random bytes and hashed using
  PBKDF2-HMAC-SHA256 with 210,000 iterations.
- Login tokens contain 256 bits of randomness.
- Only SHA-256 token hashes are stored in the database.
- Sessions expire after 24 hours.
- The desktop client keeps its token in memory only.
- The API binds to `127.0.0.1` by default.
- Profile Name can be edited immediately.
- Username changes require a six-digit, single-use email code.
- Username codes expire after 10 minutes, have a 60-second resend cooldown,
  and allow at most five incorrect attempts.

For an internet deployment, place the API behind HTTPS and use a managed database.

## Start

From `Scaccomatto_final/Scaccomatto`:

```bash
server/scripts/run.sh
```

The first run downloads H2 `2.3.232` into `server/lib/`. The database is created at:

```text
server/data/scaccomatto.mv.db
```

## Test

Keep the server running in one terminal. In another terminal:

```bash
server/scripts/smoke-test.sh
server/scripts/client-smoke-test.sh
```

The first command tests the raw HTTP API. The second uses the exact Java client
used by the game.

Then compile and run the game:

```bash
rm -rf /tmp/scaccomatto-game
mkdir -p /tmp/scaccomatto-game
javac -cp 'src:lib/*' -d /tmp/scaccomatto-game src/*.java
java -cp '/tmp/scaccomatto-game:src:lib/*' Main
```

Click `Account`, create an account, edit the display name, sign out, and sign in.

## Profile Name and Username

- **Profile Name** is the public display name and can be changed at any time.
- **Username** is the unique login handle. Changing it requires email verification.

For local development, verification messages are written to:

```text
server/data/otp-outbox.log
```

The Account screen tells you to read that file after clicking `Send Email Code`.
The file is ignored by Git and restricted to the current operating-system user
where POSIX permissions are supported.

For real email delivery, configure an SMTP server that supports implicit TLS:

```bash
SMTP_HOST='smtp.example.com' \
SMTP_PORT='465' \
SMTP_USERNAME='account@example.com' \
SMTP_PASSWORD='smtp-app-password' \
SMTP_FROM='account@example.com' \
server/scripts/run.sh
```

Use an app password or transactional mail credential, not a personal mailbox
password. For public deployment the API must also run behind HTTPS.

## Configuration

Backend:

```bash
ACCOUNT_PORT=8081 \
ACCOUNT_DB_URL='jdbc:h2:file:/absolute/path/accounts;AUTO_SERVER=FALSE' \
ACCOUNT_DB_USER='scaccomatto' \
ACCOUNT_DB_PASS='replace-with-secure-password' \
server/scripts/run.sh
```

The account server now requires `ACCOUNT_DB_USER` and `ACCOUNT_DB_PASS` to be set at startup. If they are missing, the service will fail fast with a clear error instead of falling back to `sa`/empty password.

### First admin user promotion

There is no public admin bootstrap endpoint. Promote the first administrator directly in the database once a user exists:

```sql
UPDATE users
SET is_admin = TRUE
WHERE username_normalized = 'firstadminusername';
```

Replace `firstadminusername` with the normalized username of the account to promote.

Desktop client:

```bash
SCACCOMATTO_ACCOUNT_API='http://127.0.0.1:8081' \
  java -cp '/tmp/scaccomatto-game:src:lib/*' Main
```

You can also use the JVM property:

```bash
java -Dscaccomatto.accountApi=http://127.0.0.1:8081 \
  -cp '/tmp/scaccomatto-game:src:lib/*' Main
```

## Reset Local Data

Stop the server, then run:

```bash
server/scripts/reset-database.sh
```

## API

```text
GET   /api/health
POST  /api/auth/register
POST  /api/auth/login
POST  /api/auth/logout
GET   /api/account/me
PATCH /api/account/me
POST  /api/account/username/request
POST  /api/account/username/verify
```
