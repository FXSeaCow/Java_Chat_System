package com.javachat.server.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Queries available only through role-checked admin API routes. */
public final class AdminRepository {
    private final DatabaseConfig config;

    public AdminRepository(DatabaseConfig config) { this.config = config; }

    public Map<String, Object> snapshot() throws SQLException {
        try (Connection connection = config.openConnection()) {
            connection.setReadOnly(true);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("loginHistory", rows(connection, """
                    SELECT s.id, u.display_name, u.email, COALESCE(s.device_info, 'Không rõ') AS device_info,
                           COALESCE(s.ip_address, '—') AS ip_address, s.logged_in_at,
                           CASE WHEN s.logged_out_at IS NOT NULL THEN 'Đã đăng xuất'
                                WHEN s.expires_at <= CURRENT_TIMESTAMP THEN 'Hết hạn'
                                ELSE 'Đang hoạt động' END AS session_status
                    FROM public.auth_sessions s JOIN public.users u ON u.id = s.user_id
                    ORDER BY s.logged_in_at DESC LIMIT 500
                    """));
            data.put("groups", rows(connection, """
                    SELECT g.conversation_id AS id, g.name, u.display_name AS owner_name,
                           COUNT(DISTINCT cm.user_id) FILTER (WHERE cm.left_at IS NULL) AS member_count,
                           COUNT(DISTINCT m.id) AS message_count, c.updated_at,
                           g.description
                    FROM public.group_details g
                    JOIN public.conversations c ON c.id = g.conversation_id
                    JOIN public.users u ON u.id = g.owner_id
                    LEFT JOIN public.conversation_members cm ON cm.conversation_id = g.conversation_id
                    LEFT JOIN public.messages m ON m.conversation_id = g.conversation_id
                    GROUP BY g.conversation_id, g.name, u.display_name, c.updated_at, g.description
                    ORDER BY c.updated_at DESC LIMIT 500
                    """));
            data.put("reports", rows(connection, """
                    SELECT r.id, reporter.display_name AS reporter_name, r.reason, r.details,
                           COALESCE(target.display_name, 'Tin nhắn #' || r.message_id) AS target_name,
                           r.created_at, r.status
                    FROM public.reports r
                    JOIN public.users reporter ON reporter.id = r.reporter_id
                    JOIN public.messages m ON m.id = r.message_id
                    LEFT JOIN public.users target ON target.id = m.sender_id
                    ORDER BY r.created_at DESC LIMIT 500
                    """));
            data.put("newUsers", rows(connection, """
                    SELECT id, display_name, username, email, created_at, status
                    FROM public.users WHERE role = 'USER' AND status <> 'DELETED'
                    ORDER BY created_at DESC LIMIT 500
                    """));
            data.put("users", rows(connection, """
                    SELECT u.id, u.display_name, u.username, u.email, u.status,
                           u.created_at,
                           (SELECT MAX(s.logged_in_at) FROM public.auth_sessions s
                            WHERE s.user_id = u.id) AS last_login
                    FROM public.users u
                    WHERE u.role = 'USER' AND u.status IN ('ACTIVE', 'BLOCKED')
                    ORDER BY u.created_at DESC LIMIT 500
                    """));
            data.put("activeUsers", rows(connection, """
                    SELECT u.id, u.display_name, u.username,
                           MAX(s.logged_in_at) AS last_login,
                           COUNT(DISTINCT s.id) FILTER (WHERE s.logged_out_at IS NULL
                               AND s.expires_at > CURRENT_TIMESTAMP) AS session_count,
                           (SELECT COUNT(*) FROM public.messages m WHERE m.sender_id = u.id
                               AND m.created_at >= CURRENT_DATE) AS messages_today
                    FROM public.users u JOIN public.auth_sessions s ON s.user_id = u.id
                    WHERE u.role = 'USER' AND u.status = 'ACTIVE'
                      AND s.logged_out_at IS NULL AND s.expires_at > CURRENT_TIMESTAMP
                    GROUP BY u.id, u.display_name, u.username
                    ORDER BY last_login DESC LIMIT 500
                    """));
            data.put("registrationTrend", rows(connection, """
                    SELECT day::date AS day,
                           (SELECT COUNT(*) FROM public.users u WHERE u.role = 'USER'
                               AND u.created_at >= day AND u.created_at < day + INTERVAL '1 day') AS registrations,
                           (SELECT COUNT(s.id) FROM public.auth_sessions s
                               JOIN public.users u ON u.id = s.user_id AND u.role = 'USER'
                               WHERE s.logged_in_at >= day AND s.logged_in_at < day + INTERVAL '1 day') AS logins
                    FROM generate_series(CURRENT_DATE - INTERVAL '89 days', CURRENT_DATE,
                                         INTERVAL '1 day') AS day
                    ORDER BY day
                    """));
            return data;
        }
    }

    public void updateReport(long id, long adminId, String status) throws SQLException {
        if (!"REVIEWED".equals(status) && !"DISMISSED".equals(status))
            throw new IllegalArgumentException("Trạng thái báo cáo không hợp lệ.");
        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE public.reports SET status = ?, reviewed_by = ?,
                         reviewed_at = CURRENT_TIMESTAMP(3)
                     WHERE id = ? AND status = 'PENDING'
                     """)) {
            statement.setString(1, status);
            statement.setLong(2, adminId);
            statement.setLong(3, id);
            if (statement.executeUpdate() == 0)
                throw new IllegalArgumentException("Báo cáo đã được xử lý hoặc không tồn tại.");
        }
    }

    public void updateUserStatus(long userId, String status) throws SQLException {
        if (!"ACTIVE".equals(status) && !"BLOCKED".equals(status))
            throw new IllegalArgumentException("Trạng thái tài khoản không hợp lệ.");
        try (Connection connection = config.openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE public.users SET status = ?
                        WHERE id = ? AND role = 'USER' AND status IN ('ACTIVE', 'BLOCKED')
                          AND status <> ?
                        """)) {
                    statement.setString(1, status);
                    statement.setLong(2, userId);
                    statement.setString(3, status);
                    if (statement.executeUpdate() != 1)
                        throw new IllegalArgumentException("Tài khoản không tồn tại hoặc trạng thái đã thay đổi.");
                }
                if ("BLOCKED".equals(status)) {
                    try (PreparedStatement sessions = connection.prepareStatement("""
                            UPDATE public.auth_sessions SET logged_out_at = CURRENT_TIMESTAMP(3)
                            WHERE user_id = ? AND logged_out_at IS NULL
                            """)) {
                        sessions.setLong(1, userId);
                        sessions.executeUpdate();
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            }
        }
    }

    private static List<Map<String, Object>> rows(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<Map<String, Object>> rows = new ArrayList<>();
            ResultSetMetaData meta = result.getMetaData();
            while (result.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int index = 1; index <= meta.getColumnCount(); index++) {
                    Object value = result.getObject(index);
                    if (value instanceof Timestamp timestamp) value = timestamp.toInstant().toString();
                    else if (value instanceof OffsetDateTime dateTime) value = dateTime.toInstant().toString();
                    else if (value instanceof java.sql.Date date) value = date.toLocalDate().toString();
                    row.put(meta.getColumnLabel(index), value);
                }
                rows.add(row);
            }
            return rows;
        }
    }
}
