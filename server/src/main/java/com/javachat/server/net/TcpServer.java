package com.javachat.server.net;

import com.javachat.server.database.AccountRepository;
import com.javachat.shared.dto.ProtocolMessage;
import com.javachat.shared.json.Json;
import com.javachat.shared.protocol.MessageKind;
import com.javachat.shared.protocol.MessageType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.javachat.shared.model.PresenceStatus;

/** TCP listener with a small, project-owned length-prefixed JSON protocol. */
public final class TcpServer implements AutoCloseable {
    private static final int MAX_FRAME_BYTES = 1_000_000;
    private static final int READ_TIMEOUT_MS = 60_000;

    private final ServerSocket listener;
    private final Thread acceptThread;
    private final Authentication authentication;
    private final Set<ClientSession> clients = ConcurrentHashMap.newKeySet();

    public interface Authentication {
        AccountRepository.LoginResult login(String login, String password, String ipAddress,
                                            String deviceInfo) throws SQLException;
        long authenticatedUser(String token) throws SQLException;
        void logout(String token, long userId) throws SQLException;
        default void disconnected(String token) { }
    }

    public TcpServer(InetAddress address, int port, Authentication authentication) throws IOException {
        this.authentication = authentication;
        listener = new ServerSocket(port, 50, address);
        acceptThread = Thread.ofPlatform().name("chat-tcp-accept").start(this::acceptClients);
    }

    public int port() {
        return listener.getLocalPort();
    }

    public void publishPresence(long userId, PresenceStatus status, Set<Long> recipients) {
        Map<String, Object> event = envelope(MessageType.USER_PRESENCE_CHANGED_EVENT, null,
                Map.of("userId", userId, "status", status.name(), "changedAt", Instant.now().toString()));
        for (ClientSession session : clients) {
            if (recipients.contains(session.userId)) {
                try {
                    session.send(event);
                } catch (IOException error) {
                    session.close();
                }
            }
        }
    }

    private void acceptClients() {
        while (!listener.isClosed()) {
            try {
                Socket client = listener.accept();
                Thread.ofVirtual().name("chat-tcp-client").start(() -> handleClient(client));
            } catch (IOException error) {
                if (!listener.isClosed()) System.err.println("TCP accept failed: " + error.getMessage());
            }
        }
    }

    private void handleClient(Socket client) {
        ClientSession session = new ClientSession();
        try (client;
             DataInputStream input = new DataInputStream(client.getInputStream());
             DataOutputStream output = new DataOutputStream(client.getOutputStream())) {
            session.socket = client;
            session.output = output;
            clients.add(session);
            client.setSoTimeout(READ_TIMEOUT_MS);
            while (true) {
                int length;
                try {
                    length = input.readInt();
                } catch (EOFException disconnected) {
                    return;
                }
                if (length <= 0 || length > MAX_FRAME_BYTES) return;
                byte[] bytes = new byte[length];
                input.readFully(bytes);
                Map<String, Object> response;
                UUID requestId = null;
                try {
                    Map<String, Object> request = Json.object(
                            Json.parse(new String(bytes, StandardCharsets.UTF_8)));
                    if (request.get("requestId") instanceof String id) {
                        try {
                            requestId = UUID.fromString(id);
                        } catch (IllegalArgumentException ignored) {
                            // The request itself will be rejected below.
                        }
                    }
                    response = dispatch(request, session, client.getInetAddress().getHostAddress());
                } catch (IllegalArgumentException error) {
                    response = envelope(MessageType.ERROR_RESPONSE, requestId,
                            Map.of("error", "Invalid request"));
                } catch (SQLException error) {
                    System.err.println("TCP database error SQLSTATE " + error.getSQLState());
                    response = envelope(MessageType.ERROR_RESPONSE, requestId,
                            Map.of("error", "Authentication service unavailable"));
                }
                session.send(response);
            }
        } catch (IOException error) {
            // Disconnects and idle timeouts affect only this client.
        } finally {
            clients.remove(session);
            if (session.token != null && clients.stream()
                    .noneMatch(other -> session.token.equals(other.token))) {
                authentication.disconnected(session.token);
            }
        }
    }

    private Map<String, Object> dispatch(Map<String, Object> request, ClientSession session,
                                         String ipAddress) throws SQLException {
        UUID requestId = UUID.fromString(requiredString(request, "requestId"));
        String version = requiredString(request, "version");
        MessageType type = MessageType.valueOf(requiredString(request, "type"));
        if (type.getKind() != MessageKind.REQUEST) {
            return envelope(MessageType.ERROR_RESPONSE, requestId, Map.of("error", "Expected a request type"));
        }
        if (!ProtocolMessage.CURRENT_VERSION.equals(version)) {
            return envelope(MessageType.ERROR_RESPONSE, requestId, Map.of("error", "Unsupported protocol version"));
        }
        if (type == MessageType.SYSTEM_HELLO_REQUEST) {
            Object token = request.get("authToken");
            if (token != null) {
                long userId = token instanceof String text ? authentication.authenticatedUser(text) : -1;
                if (userId < 0) {
                    session.token = null;
                    session.userId = -1;
                    return error(requestId, "Invalid or expired session");
                }
                session.token = (String) token;
                session.userId = userId;
            }
            return envelope(MessageType.SYSTEM_HELLO_RESPONSE, requestId,
                    Map.of("version", ProtocolMessage.CURRENT_VERSION, "authenticated", session.token != null));
        }
        if (type == MessageType.AUTH_LOGIN_REQUEST) {
            session.token = null;
            session.userId = -1;
            Map<String, Object> payload = Json.object(request.get("payload"));
            try {
                AccountRepository.LoginResult result = authentication.login(
                        requiredString(payload, "login"), requiredString(payload, "password"),
                        ipAddress, optionalString(payload, "deviceInfo"));
                session.token = result.token();
                session.userId = result.userId();
                return envelope(MessageType.AUTH_LOGIN_RESPONSE, requestId,
                        Map.of("token", result.token(), "userId", result.userId()));
            } catch (AccountRepository.AuthenticationException error) {
                return error(requestId, "Invalid credentials");
            }
        }
        long authenticatedId = session.token == null ? -1 : authentication.authenticatedUser(session.token);
        if (authenticatedId < 0) {
            session.token = null;
            session.userId = -1;
            return error(requestId, "Authentication required or session expired");
        }
        if (type == MessageType.AUTH_LOGOUT_REQUEST) {
            authentication.logout(session.token, authenticatedId);
            session.token = null;
            session.userId = -1;
            return envelope(MessageType.AUTH_LOGOUT_RESPONSE, requestId, Map.of("ok", true));
        }
        if (type == MessageType.SYSTEM_PING) {
            return envelope(MessageType.SYSTEM_PONG, requestId, Map.of("ok", true));
        }
        return error(requestId, "Request type is not implemented on TCP");
    }

    private static String optionalString(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value == null) return null;
        if (!(value instanceof String text)) throw new IllegalArgumentException("Invalid text: " + key);
        return text;
    }

    private static Map<String, Object> error(UUID requestId, String detail) {
        return envelope(MessageType.ERROR_RESPONSE, requestId, Map.of("error", detail));
    }

    private static final class ClientSession {
        private volatile String token;
        private volatile long userId = -1;
        private Socket socket;
        private DataOutputStream output;

        private synchronized void send(Map<String, Object> message) throws IOException {
            byte[] bytes = Json.stringify(message).getBytes(StandardCharsets.UTF_8);
            output.writeInt(bytes.length);
            output.write(bytes);
            output.flush();
        }

        private void close() {
            try { socket.close(); } catch (IOException ignored) { }
        }
    }

    private static String requiredString(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("Missing text: " + key);
        }
        return text;
    }

    private static Map<String, Object> envelope(MessageType type, UUID requestId, Object payload) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("version", ProtocolMessage.CURRENT_VERSION);
        message.put("type", type.name());
        message.put("requestId", requestId == null ? null : requestId.toString());
        message.put("timestamp", Instant.now().toString());
        message.put("authToken", null);
        message.put("payload", payload);
        return message;
    }

    @Override
    public void close() throws IOException {
        listener.close();
        acceptThread.interrupt();
    }
}
