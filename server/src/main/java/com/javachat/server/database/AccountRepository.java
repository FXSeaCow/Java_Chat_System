package com.javachat.server.database;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
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
        String name = displayName.strip();
        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        if (name.length() < 2 || name.length() > 100)
            throw new IllegalArgumentException("Họ tên phải có từ 2 đến 100 ký tự.");
        if (normalizedEmail.length() > 255 ||
                !normalizedEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("Email không hợp lệ.");
        if (password.length() < 8 || password.length() > 72)
            throw new IllegalArgumentException("Mật khẩu phải có từ 8 đến 72 ký tự.");

        byte[] random = new byte[16];
        new SecureRandom().nextBytes(random);
        String username = "user_" + HexFormat.of().formatHex(random);
        String passwordHash = hashPassword(password);
        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO public.users (username, email, password_hash, display_name)
                     VALUES (?, ?, ?, ?) RETURNING id
                     """)) {
            statement.setString(1, username);
            statement.setString(2, normalizedEmail);
            statement.setString(3, passwordHash);
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
            super("Sai email/tên đăng nhập hoặc mật khẩu.");
        }
    }

    public LoginResult login(String login, String password) throws SQLException {
        if (login == null || login.isBlank() || password == null || password.isEmpty())
            throw new AuthenticationException();
        try (Connection connection = config.openConnection()) {
            connection.setAutoCommit(false);
            try {
                long userId;
                String stored;
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, password_hash FROM public.users
                        WHERE (lower(email) = lower(?) OR lower(username) = lower(?))
                          AND status = 'ACTIVE' AND role = 'USER' FOR UPDATE
                        """)) {
                    statement.setString(1, login.strip());
                    statement.setString(2, login.strip());
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new AuthenticationException();
                        userId = result.getLong("id");
                        stored = result.getString("password_hash");
                    }
                }
                if (!verifyPassword(connection, password, stored)) throw new AuthenticationException();
                byte[] tokenBytes = new byte[32];
                new SecureRandom().nextBytes(tokenBytes);
                String token = HexFormat.of().formatHex(tokenBytes);
                try (PreparedStatement session = connection.prepareStatement("""
                        INSERT INTO public.auth_sessions (user_id, token_hash, expires_at)
                        VALUES (?, ?, ?)
                        """)) {
                    session.setLong(1, userId);
                    session.setString(2, sha256(token));
                    session.setTimestamp(3, Timestamp.from(Instant.now().plus(7, ChronoUnit.DAYS)));
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
        if (token == null || !token.matches("[0-9a-f]{64}")) return -1;
        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT s.user_id FROM public.auth_sessions s
                     JOIN public.users u ON u.id = s.user_id
                     WHERE s.token_hash = ? AND s.logged_out_at IS NULL
                       AND s.expires_at > CURRENT_TIMESTAMP
                       AND u.status = 'ACTIVE' AND u.role = 'USER'
                     """)) {
            statement.setString(1, sha256(token));
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
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE public.users SET password_hash = ? WHERE id = ?
                        """)) {
                    statement.setString(1, hashPassword(newPassword));
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
                connection.commit();
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            }
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
            // Existing seed accounts use PostgreSQL pgcrypto bcrypt hashes.
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

    private static String hashPassword(String password) {
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
