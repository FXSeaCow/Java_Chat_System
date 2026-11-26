package com.javachat.server.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Database operations for one local development account. */
public final class ChatRepository {
    private final DatabaseConfig config;
    private final long currentUserId;

    public ChatRepository(DatabaseConfig config, long currentUserId) {
        this.config = config;
        this.currentUserId = currentUserId;
    }

    public Map<String, Object> snapshot() throws SQLException {
        try (Connection connection = config.openConnection()) {
            connection.setReadOnly(true);
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.setAutoCommit(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("currentUserId", currentUserId);
            result.put("users", rows(connection, """
                    SELECT id, username, email, display_name, avatar_url, address,
                           date_of_birth, gender, role, status, created_at, updated_at
                    FROM public.users WHERE status <> 'DELETED' ORDER BY id
                    """));
            result.put("friendships", rows(connection, """
                    SELECT id, requester_id, addressee_id, status, created_at, responded_at
                    FROM public.friendships
                    WHERE requester_id = ? OR addressee_id = ? ORDER BY id
                    """, currentUserId, currentUserId));
            String mine = """
                    SELECT conversation_id FROM public.conversation_members
                    WHERE user_id = ? AND left_at IS NULL
                    """;
            result.put("conversations", rows(connection,
                    "SELECT id, type, created_by, created_at, updated_at FROM public.conversations "
                            + "WHERE id IN (" + mine + ") ORDER BY updated_at DESC", currentUserId));
            result.put("members", rows(connection,
                    "SELECT conversation_id, user_id, member_role, joined_at, left_at, last_read_at "
                            + "FROM public.conversation_members WHERE conversation_id IN (" + mine + ") "
                            + "ORDER BY conversation_id, joined_at", currentUserId));
            result.put("groups", rows(connection,
                    "SELECT conversation_id, owner_id, name, avatar_url, description, created_at "
                            + "FROM public.group_details WHERE conversation_id IN (" + mine + ") "
                            + "ORDER BY conversation_id", currentUserId));
            result.put("messages", rows(connection,
                    "SELECT id, conversation_id, sender_id, reply_to_id, content, message_type, "
                            + "status, created_at, edited_at FROM public.messages "
                            + "WHERE conversation_id IN (" + mine + ") ORDER BY created_at, id", currentUserId));
            result.put("attachments", rows(connection,
                    "SELECT a.id, a.message_id, a.file_name, a.file_url, a.mime_type, a.file_size "
                            + "FROM public.message_attachments a JOIN public.messages m ON m.id = a.message_id "
                            + "WHERE m.conversation_id IN (" + mine + ") ORDER BY a.id", currentUserId));
            connection.commit();
            return result;
        }
    }

    public List<Long> onlineFriendIds(Set<Long> onlineUserIds) throws SQLException {
        if (onlineUserIds.isEmpty()) return List.of();
        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT CASE WHEN requester_id = ? THEN addressee_id ELSE requester_id END AS friend_id
                     FROM public.friendships
                     WHERE status = 'ACCEPTED' AND (requester_id = ? OR addressee_id = ?)
                     ORDER BY friend_id
                     """)) {
            statement.setLong(1, currentUserId);
            statement.setLong(2, currentUserId);
            statement.setLong(3, currentUserId);
            List<Long> result = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    long friendId = rows.getLong(1);
                    if (onlineUserIds.contains(friendId)) result.add(friendId);
                }
            }
            return result;
        }
    }

    public long sendMessage(long conversationId, Long replyToId, String content) throws SQLException {
        if (content == null || content.isBlank() || content.length() > 10000)
            throw new IllegalArgumentException("Message must contain 1 to 10000 characters");
        try (Connection connection = config.openConnection()) {
            connection.setAutoCommit(false);
            try {
                requireMember(connection, conversationId);
                if (replyToId != null && rows(connection,
                        "SELECT id FROM public.messages WHERE id = ? AND conversation_id = ?",
                        replyToId, conversationId).isEmpty())
                    throw new IllegalArgumentException("Reply target is outside this conversation");
                long id = insertId(connection, """
                        INSERT INTO public.messages (conversation_id, sender_id, reply_to_id, content)
                        VALUES (?, ?, ?, ?) RETURNING id
                        """, conversationId, currentUserId, replyToId, content.strip());
                execute(connection, "UPDATE public.conversations SET updated_at = CURRENT_TIMESTAMP(3) WHERE id = ?",
                        conversationId);
                connection.commit();
                return id;
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            }
        }
    }

    public void requestFriend(long addresseeId) throws SQLException {
        if (addresseeId == currentUserId) throw new IllegalArgumentException("Cannot add yourself");
        try (Connection connection = config.openConnection()) {
            if (rows(connection, "SELECT id FROM public.users WHERE id = ? AND role = 'USER' AND status = 'ACTIVE'",
                    addresseeId).isEmpty()) throw new IllegalArgumentException("User is unavailable");
            execute(connection, """
                    INSERT INTO public.friendships (requester_id, addressee_id)
                    VALUES (?, ?)
                    """, currentUserId, addresseeId);
        }
    }

    public void respondFriend(long friendshipId, boolean accept) throws SQLException {
        try (Connection connection = config.openConnection()) {
            connection.setAutoCommit(false);
            try {
                List<Map<String, Object>> found = rows(connection, """
                        SELECT requester_id FROM public.friendships
                        WHERE id = ? AND addressee_id = ? AND status = 'PENDING' FOR UPDATE
                        """, friendshipId, currentUserId);
                if (found.isEmpty()) throw new IllegalArgumentException("Friend request unavailable");
                long requesterId = ((Number) found.get(0).get("requester_id")).longValue();
                if (accept) {
                    execute(connection, """
                            UPDATE public.friendships SET status = 'ACCEPTED',
                                responded_at = CURRENT_TIMESTAMP(3) WHERE id = ?
                            """, friendshipId);
                    if (rows(connection, """
                            SELECT c.id FROM public.conversations c
                            JOIN public.conversation_members a ON a.conversation_id = c.id
                            JOIN public.conversation_members b ON b.conversation_id = c.id
                            WHERE c.type = 'DIRECT' AND a.user_id = ? AND b.user_id = ?
                              AND a.left_at IS NULL AND b.left_at IS NULL
                            """, currentUserId, requesterId).isEmpty()) {
                        long id = insertId(connection, """
                                INSERT INTO public.conversations (type, created_by)
                                VALUES ('DIRECT', ?) RETURNING id
                                """, currentUserId);
                        execute(connection, """
                                INSERT INTO public.conversation_members (conversation_id, user_id)
                                VALUES (?, ?), (?, ?)
                                """, id, currentUserId, id, requesterId);
                    }
                } else {
                    execute(connection, "DELETE FROM public.friendships WHERE id = ?", friendshipId);
                }
                connection.commit();
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            }
        }
    }

    public long createGroup(String name, String description, List<Long> memberIds) throws SQLException {
        if (name == null || name.isBlank() || name.length() > 100)
            throw new IllegalArgumentException("Group name must contain 1 to 100 characters");
        if (memberIds == null || memberIds.isEmpty())
            throw new IllegalArgumentException("Select at least one member");
        try (Connection connection = config.openConnection()) {
            connection.setAutoCommit(false);
            try {
                long id = insertId(connection, """
                        INSERT INTO public.conversations (type, created_by)
                        VALUES ('GROUP', ?) RETURNING id
                        """, currentUserId);
                execute(connection, """
                        INSERT INTO public.group_details (conversation_id, owner_id, name, description)
                        VALUES (?, ?, ?, ?)
                        """, id, currentUserId, name.strip(), description);
                execute(connection, """
                        INSERT INTO public.conversation_members (conversation_id, user_id, member_role)
                        VALUES (?, ?, 'OWNER')
                        """, id, currentUserId);
                for (long memberId : memberIds.stream().distinct().toList()) {
                    if (memberId == currentUserId) continue;
                    if (rows(connection, """
                            SELECT id FROM public.users WHERE id = ? AND role = 'USER' AND status = 'ACTIVE'
                            """, memberId).isEmpty()) throw new IllegalArgumentException("Group member unavailable");
                    execute(connection, """
                            INSERT INTO public.conversation_members (conversation_id, user_id)
                            VALUES (?, ?)
                            """, id, memberId);
                }
                connection.commit();
                return id;
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            }
        }
    }

    public void updateGroup(long conversationId, String name, String description) throws SQLException {
        if (name == null || name.isBlank() || name.length() > 100)
            throw new IllegalArgumentException("Group name must contain 1 to 100 characters");
        try (Connection connection = config.openConnection()) {
            int count = execute(connection, """
                    UPDATE public.group_details SET name = ?, description = ?
                    WHERE conversation_id = ? AND owner_id = ?
                    """, name.strip(), description, conversationId, currentUserId);
            if (count == 0) throw new IllegalArgumentException("Group unavailable or not owned by you");
        }
    }

    public void removeGroupMember(long conversationId, long userId) throws SQLException {
        try (Connection connection = config.openConnection()) {
            requireOwner(connection, conversationId);
            int count = execute(connection, """
                    UPDATE public.conversation_members SET left_at = CURRENT_TIMESTAMP(3)
                    WHERE conversation_id = ? AND user_id = ? AND member_role <> 'OWNER'
                      AND left_at IS NULL
                    """, conversationId, userId);
            if (count == 0) throw new IllegalArgumentException("Member unavailable");
        }
    }

    public void changeGroupRole(long conversationId, long userId, String role) throws SQLException {
        if (!"ADMIN".equals(role) && !"MEMBER".equals(role))
            throw new IllegalArgumentException("Invalid group role");
        try (Connection connection = config.openConnection()) {
            requireOwner(connection, conversationId);
            int count = execute(connection, """
                    UPDATE public.conversation_members SET member_role = ?
                    WHERE conversation_id = ? AND user_id = ? AND member_role <> 'OWNER'
                      AND left_at IS NULL
                    """, role, conversationId, userId);
            if (count == 0) throw new IllegalArgumentException("Member unavailable");
        }
    }

    private void requireMember(Connection connection, long conversationId) throws SQLException {
        if (rows(connection, """
                SELECT conversation_id FROM public.conversation_members
                WHERE conversation_id = ? AND user_id = ? AND left_at IS NULL
                """, conversationId, currentUserId).isEmpty())
            throw new IllegalArgumentException("Conversation unavailable");
    }

    private void requireOwner(Connection connection, long conversationId) throws SQLException {
        if (rows(connection, """
                SELECT conversation_id FROM public.group_details
                WHERE conversation_id = ? AND owner_id = ?
                """, conversationId, currentUserId).isEmpty())
            throw new IllegalArgumentException("Group unavailable or not owned by you");
    }

    private static long insertId(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = prepare(connection, sql, parameters);
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) throw new SQLException("Insert returned no ID");
            return result.getLong(1);
        }
    }

    private static int execute(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = prepare(connection, sql, parameters)) {
            return statement.executeUpdate();
        }
    }

    private static List<Map<String, Object>> rows(Connection connection, String sql,
                                                   Object... parameters) throws SQLException {
        try (PreparedStatement statement = prepare(connection, sql, parameters);
             ResultSet result = statement.executeQuery()) {
            List<Map<String, Object>> rows = new ArrayList<>();
            ResultSetMetaData meta = result.getMetaData();
            while (result.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    Object value = result.getObject(i);
                    if (value instanceof java.sql.Timestamp timestamp) value = timestamp.toInstant().toString();
                    else if (value instanceof OffsetDateTime dateTime) value = dateTime.toInstant().toString();
                    else if (value instanceof java.sql.Date date) value = date.toLocalDate().toString();
                    else if (value instanceof LocalDate date) value = date.toString();
                    else if (value instanceof Instant instant) value = instant.toString();
                    row.put(meta.getColumnLabel(i), value);
                }
                rows.add(row);
            }
            return rows;
        }
    }

    private static PreparedStatement prepare(Connection connection, String sql,
                                             Object... parameters) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        try {
            for (int i = 0; i < parameters.length; i++) statement.setObject(i + 1, parameters[i]);
            return statement;
        } catch (SQLException | RuntimeException error) {
            statement.close();
            throw error;
        }
    }
}
