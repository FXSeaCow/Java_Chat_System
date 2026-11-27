package com.javachat.server.database;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;

/** Registration for the local desktop API. */
public final class AccountRepository {
    private static final int HASH_ITERATIONS = 210_000;
    private final DatabaseConfig config;

    public AccountRepository(DatabaseConfig config) {
        this.config = config;
    }

    public long register(String displayName, String email, String password) throws SQLException {
        String passwordError = com.javachat.shared.model.RegistrationPassword.error(password);
        if (passwordError != null) throw new IllegalArgumentException(passwordError);
        byte[] random = new byte[16];
        new SecureRandom().nextBytes(random);
        return registerWithUsername(displayName, "user_" + HexFormat.of().formatHex(random), email, password);
    }

    public long registerWithUsername(String displayName, String username, String email, String password)
            throws SQLException {
        String name = displayName.strip();
        String normalizedUsername = username.strip();
        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        if (name.isEmpty())
            throw new IllegalArgumentException("Vui lòng nhập họ và tên.");
        if (name.length() > 100)
            throw new IllegalArgumentException("Họ và tên không được vượt quá 100 ký tự.");
        if (!normalizedUsername.matches("[A-Za-z0-9._-]{3,50}"))
            throw new IllegalArgumentException("Tên đăng nhập cần 3–50 ký tự chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.");
        if (normalizedEmail.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập email.");
        if (normalizedEmail.length() > 255 ||
                !normalizedEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("Vui lòng nhập email hợp lệ.");
        if (password == null || password.isEmpty())
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu.");
        if (password.length() < 8)
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự.");
        if (password.length() > 72)
            throw new IllegalArgumentException("Mật khẩu không được vượt quá 72 ký tự.");

        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO public.users (username, email, password_hash, display_name)
                     VALUES (?, ?, ?, ?) RETURNING id
                     """)) {
            statement.setString(1, normalizedUsername);
            statement.setString(2, normalizedEmail);
            statement.setString(3, hashPassword(connection, password));
            statement.setString(4, name);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Registration returned no user ID");
                return result.getLong(1);
            }
        }
    }

    public record LoginResult(String token, long userId) { }

    public static final class AuthenticationException extends RuntimeException {
        public AuthenticationException() {
            super("Email, tên đăng nhập hoặc mật khẩu không đúng.");
        }
    }

    public LoginResult login(String login, String password) throws SQLException {
        return login(login, password, "USER", null, null);
    }

    public LoginResult loginAdmin(String login, String password) throws SQLException {
        return login(login, password, "ADMIN", null, null);
    }

    public LoginResult login(String login, String password, String ipAddress, String deviceInfo) throws SQLException {
        return login(login, password, "USER", ipAddress, deviceInfo);
    }

    public LoginResult loginAdmin(String login, String password, String ipAddress, String deviceInfo) throws SQLException {
        return login(login, password, "ADMIN", ipAddress, deviceInfo);
    }

    private LoginResult login(String login, String password, String role,
                              String ipAddress, String deviceInfo) throws SQLException {
        if (login == null || login.isBlank() || password == null || password.isEmpty())
            throw new AuthenticationException();
        try (Connection connection = config.openConnection()) {
            connection.setAutoCommit(false);
            try {
                long userId;
                String stored;
                String resetHash;
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, password_hash,
                            CASE WHEN reset_password_expires_at > CURRENT_TIMESTAMP THEN reset_password_hash END
                                AS valid_reset_hash
                        FROM public.users
                        WHERE (lower(email) = lower(?) OR lower(username) = lower(?))
                          AND status = 'ACTIVE' AND role = ? FOR UPDATE
                        """)) {
                    statement.setString(1, login.strip());
                    statement.setString(2, login.strip());
                    statement.setString(3, role);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new AuthenticationException();
                        userId = result.getLong("id");
                        stored = result.getString("password_hash");
                        resetHash = result.getString("valid_reset_hash");
                    }
                }
                if (!verifyPassword(connection, password, stored)) {
                    if (resetHash == null || !verifyPassword(connection, password, resetHash))
                        throw new AuthenticationException();
                    replacePassword(connection, userId, password);
                } else if (stored.startsWith("pbkdf2_sha256$") && supportsBcrypt(password)) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE public.users SET password_hash = ? WHERE id = ?")) {
                        update.setString(1, hashPassword(connection, password));
                        update.setLong(2, userId);
                        update.executeUpdate();
                    }
                }
                byte[] tokenBytes = new byte[32];
                new SecureRandom().nextBytes(tokenBytes);
                String token = HexFormat.of().formatHex(tokenBytes);
                try (PreparedStatement session = connection.prepareStatement("""
                        INSERT INTO public.auth_sessions (user_id, token_hash, expires_at, ip_address, device_info)
                        VALUES (?, ?, ?, ?, ?)
                        """)) {
                    session.setLong(1, userId);
                    session.setString(2, sha256(token));
                    session.setTimestamp(3, Timestamp.from(Instant.now().plus(7, ChronoUnit.DAYS)));
                    session.setString(4, ipAddress);
                    session.setString(5, deviceInfo == null ? null : deviceInfo.substring(0, Math.min(500, deviceInfo.length())));
                    session.executeUpdate();
                }
                connection.commit();
                return new LoginResult(token, userId);
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            }
        }
    }

    public long authenticatedUser(String token) throws SQLException {
        return authenticated(token, "USER");
    }

    public long authenticatedAdmin(String token) throws SQLException {
        return authenticated(token, "ADMIN");
    }

    private long authenticated(String token, String role) throws SQLException {
        if (token == null || !token.matches("[0-9a-f]{64}")) return -1;
        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT s.user_id FROM public.auth_sessions s
                     JOIN public.users u ON u.id = s.user_id
                     WHERE s.token_hash = ? AND s.logged_out_at IS NULL
                       AND s.expires_at > CURRENT_TIMESTAMP
                       AND u.status = 'ACTIVE' AND u.role = ?
                     """)) {
            statement.setString(1, sha256(token));
            statement.setString(2, role);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : -1;
            }
        }
    }

    public void logout(String token, long userId) throws SQLException {
        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE public.auth_sessions
                     SET logged_out_at = CURRENT_TIMESTAMP(3)
                     WHERE token_hash = ? AND user_id = ? AND logged_out_at IS NULL
                     """)) {
            statement.setString(1, sha256(token));
            statement.setLong(2, userId);
            statement.executeUpdate();
        }
    }

    public void changePassword(long userId, String currentPassword, String newPassword) throws SQLException {
        if (newPassword.length() < 8 || newPassword.length() > 72)
            throw new IllegalArgumentException("Mật khẩu mới phải có từ 8 đến 72 ký tự.");
        if (newPassword.equals(currentPassword))
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        try (Connection connection = config.openConnection()) {
            connection.setAutoCommit(false);
            try {
                String stored;
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT password_hash FROM public.users
                        WHERE id = ? AND status = 'ACTIVE' FOR UPDATE
                        """)) {
                    statement.setLong(1, userId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("Tài khoản không khả dụng.");
                        stored = result.getString(1);
                    }
                }
                if (!verifyPassword(connection, currentPassword, stored))
                    throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
                replacePassword(connection, userId, newPassword);
                connection.commit();
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            }
        }
    }

    public void recordAppLaunch(java.util.UUID launchId, Instant openedAt) throws SQLException {
        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO public.app_launches (launch_id, opened_at) VALUES (?, ?)
                     ON CONFLICT DO NOTHING
                     """)) {
            statement.setObject(1, launchId);
            statement.setTimestamp(2, Timestamp.from(openedAt));
            statement.executeUpdate();
        }
    }

    public void recordAppOpen(long userId, java.util.UUID launchId, Instant openedAt) throws SQLException {
        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO public.app_open_events (user_id, launch_id, opened_at) VALUES (?, ?, ?)
                     ON CONFLICT DO NOTHING
                     """)) {
            statement.setLong(1, userId);
            statement.setObject(2, launchId);
            statement.setTimestamp(3, Timestamp.from(openedAt));
            statement.executeUpdate();
        }
    }

    @FunctionalInterface
    public interface PasswordDelivery {
        void send(String password) throws java.io.IOException, InterruptedException;
    }

    public void resetPassword(String email, PasswordDelivery delivery)
            throws SQLException, java.io.IOException, InterruptedException {
        byte[] secret = new byte[18];
        new SecureRandom().nextBytes(secret);
        String password = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE public.users SET reset_password_hash = ?,
                         reset_password_expires_at = CURRENT_TIMESTAMP + INTERVAL '30 minutes'
                     WHERE lower(email) = lower(?) AND role = 'USER' AND status = 'ACTIVE'
                     """)) {
            statement.setString(1, hashPassword(connection, password));
            statement.setString(2, email.strip());
            if (statement.executeUpdate() == 0) return;
        }
        delivery.send(password);
    }

    private static void replacePassword(Connection connection, long userId, String password) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                UPDATE public.users SET password_hash = ?, reset_password_hash = NULL,
                    reset_password_expires_at = NULL WHERE id = ?
                """)) {
            statement.setString(1, hashPassword(connection, password));
            statement.setLong(2, userId);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE public.auth_sessions SET logged_out_at = CURRENT_TIMESTAMP(3)
                WHERE user_id = ? AND logged_out_at IS NULL
                """)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        }
    }

    private static boolean verifyPassword(Connection connection, String password, String stored)
            throws SQLException {
        if (stored.startsWith("pbkdf2_sha256$")) {
            try {
                String[] parts = stored.split("\\$");
                if (parts.length != 4) return false;
                int iterations = Integer.parseInt(parts[1]);
                if (iterations < 100_000 || iterations > 1_000_000) return false;
                byte[] salt = HexFormat.of().parseHex(parts[2]);
                byte[] expected = HexFormat.of().parseHex(parts[3]);
                PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations,
                        expected.length * 8);
                try {
                    byte[] actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                            .generateSecret(spec).getEncoded();
                    return MessageDigest.isEqual(expected, actual);
                } finally {
                    spec.clearPassword();
                }
            } catch (Exception error) {
                return false;
            }
        }
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$")) {
            if (!supportsBcrypt(password)) return false;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT ? = extensions.crypt(?, ?)")) {
                statement.setString(1, stored);
                statement.setString(2, password);
                statement.setString(3, stored);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() && result.getBoolean(1);
                }
            }
        }
        return false;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    private static boolean supportsBcrypt(String password) {
        return password.indexOf(0) < 0 && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    private static String hashPassword(Connection connection, String password) throws SQLException {
        if (!supportsBcrypt(password)) return hashPbkdf2(password);
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT extensions.crypt(?, extensions.gen_salt('bf', 12))")) {
            statement.setString(1, password);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("BCrypt returned no hash");
                return result.getString(1);
            }
        }
    }

    private static String hashPbkdf2(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_ITERATIONS, 256);
        try {
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return "pbkdf2_sha256$" + HASH_ITERATIONS + "$"
                    + HexFormat.of().formatHex(salt) + "$" + HexFormat.of().formatHex(hash);
        } catch (Exception error) {
            throw new IllegalStateException("Unable to hash password", error);
        } finally {
            spec.clearPassword();
        }
    }
}
