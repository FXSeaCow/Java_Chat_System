package com.javachat.server;

import com.javachat.server.database.ChatRepository;
import com.javachat.server.database.AccountRepository;
import com.javachat.server.database.AdminRepository;
import com.javachat.server.database.DatabaseConfig;
import com.javachat.server.net.TcpServer;
import com.javachat.shared.json.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Loopback-only authenticated API for the desktop client. */
public final class ServerApp {
    private static final PresenceTracker PRESENCE = new PresenceTracker();
    private ServerApp() { }

    public static void main(String[] args) throws Exception {
        Properties settings = new Properties();
        Path path = Path.of(System.getenv().getOrDefault("JAVA_CHAT_CONFIG", "config.properties"));
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                settings.load(reader);
            }
        }
        String host = settings.getProperty("server.host", "127.0.0.1").trim();
        int port = Integer.parseInt(System.getenv().getOrDefault(
                "JAVA_CHAT_SERVER_PORT", settings.getProperty("server.port", "8080")).trim());
        InetAddress address = InetAddress.getByName(host);
        if (!address.isLoopbackAddress()) {
            throw new IllegalArgumentException("Desktop API must bind to loopback");
        }
        DatabaseConfig databaseConfig = DatabaseConfig.load();
        AccountRepository accounts = new AccountRepository(databaseConfig);
        HttpServer server = HttpServer.create(new InetSocketAddress(address, port), 0);
        server.createContext("/api/", exchange -> handle(exchange, databaseConfig, accounts));
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        int tcpPort = Integer.parseInt(System.getenv().getOrDefault(
                "JAVA_CHAT_TCP_PORT", settings.getProperty("server.tcp.port", "9090")).trim());
        TcpServer tcpServer = new TcpServer(address, tcpPort, new TcpServer.Authentication() {
            @Override
            public AccountRepository.LoginResult login(String login, String password,
                                                       String ipAddress, String deviceInfo) throws SQLException {
                AccountRepository.LoginResult result = accounts.login(login, password, ipAddress, deviceInfo);
                PRESENCE.online(result.userId(), result.token());
                return result;
            }

            @Override
            public long authenticatedUser(String token) throws SQLException {
                long userId = accounts.authenticatedUser(token);
                if (userId >= 0) PRESENCE.online(userId, token);
                return userId;
            }

            @Override
            public void logout(String token, long userId) throws SQLException {
                accounts.logout(token, userId);
                PRESENCE.offline(token);
            }

            @Override
            public void disconnected(String token) {
                PRESENCE.offline(token);
            }
        });
        PRESENCE.setListener((userId, status) -> {
            try {
                Set<Long> friends = Set.copyOf(new ChatRepository(databaseConfig, userId)
                        .onlineFriendIds(PRESENCE.onlineUserIds()));
                tcpServer.publishPresence(userId, status, friends);
            } catch (SQLException error) {
                System.err.println("Presence broadcast failed SQLSTATE " + error.getSQLState());
            }
        });
        var presenceSweep = Executors.newSingleThreadScheduledExecutor(runnable ->
                Thread.ofVirtual().name("chat-presence-sweep").unstarted(runnable));
        presenceSweep.scheduleAtFixedRate(PRESENCE::onlineUserIds, 15, 15, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            presenceSweep.shutdownNow();
            try {
                tcpServer.close();
            } catch (IOException error) {
                System.err.println("TCP shutdown failed: " + error.getMessage());
            }
        }));
        server.start();
        System.out.println("E-Chat API listening at http://" + host + ":" + server.getAddress().getPort());
        System.out.println("E-Chat TCP listening at " + host + ":" + tcpServer.port());
    }

    private static void handle(HttpExchange exchange, DatabaseConfig databaseConfig,
                               AccountRepository accounts) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if ("GET".equals(method) && "/api/snapshot".equals(path)) {
                long userId = authenticatedUser(exchange, accounts);
                if (userId < 0) return;
                ChatRepository repository = new ChatRepository(databaseConfig, userId);
                Map<String, Object> snapshot = repository.snapshot();
                snapshot.put("onlineFriendIds", repository.onlineFriendIds(PRESENCE.onlineUserIds()));
                respond(exchange, 200, snapshot);
                return;
            }
            if ("GET".equals(method) && "/api/presence".equals(path)) {
                long userId = authenticatedUser(exchange, accounts);
                if (userId < 0) return;
                respond(exchange, 200, Map.of("onlineFriendIds",
                        new ChatRepository(databaseConfig, userId).onlineFriendIds(PRESENCE.onlineUserIds())));
                return;
            }
            if ("GET".equals(method) && "/api/admin/snapshot".equals(path)) {
                long adminId = authenticatedAdmin(exchange, accounts);
                if (adminId < 0) return;
                respond(exchange, 200, new AdminRepository(databaseConfig).snapshot());
                return;
            }
            if (!"POST".equals(method)) {
                respond(exchange, 404, Map.of("error", "Unknown API endpoint"));
                return;
            }
            byte[] bytes = exchange.getRequestBody().readNBytes(1_000_001);
            if (bytes.length > 1_000_000) {
                respond(exchange, 413, Map.of("error", "Request too large"));
                return;
            }
            Map<String, Object> body = Json.object(Json.parse(new String(bytes, StandardCharsets.UTF_8)));
            if ("/api/auth/register".equals(path)) {
                respond(exchange, 201, Map.of("userId", accounts.register(
                        string(body, "displayName"), string(body, "email"), string(body, "password"))));
                return;
            }
            if ("/api/auth/login".equals(path)) {
                AccountRepository.LoginResult login = accounts.login(
                        string(body, "login"), string(body, "password"),
                        exchange.getRemoteAddress().getAddress().getHostAddress(),
                        exchange.getRequestHeaders().getFirst("User-Agent"));
                PRESENCE.online(login.userId(), login.token());
                respond(exchange, 200, Map.of("token", login.token(), "userId", login.userId(),
                        "onlineFriendIds", new ChatRepository(databaseConfig, login.userId())
                                .onlineFriendIds(PRESENCE.onlineUserIds())));
                return;
            }
            if ("/api/admin/auth/login".equals(path)) {
                AccountRepository.LoginResult login = accounts.loginAdmin(
                        string(body, "login"), string(body, "password"),
                        exchange.getRemoteAddress().getAddress().getHostAddress(),
                        exchange.getRequestHeaders().getFirst("User-Agent"));
                respond(exchange, 200, Map.of("token", login.token(), "userId", login.userId()));
                return;
            }
            if (path.startsWith("/api/admin/")) {
                long adminId = authenticatedAdmin(exchange, accounts);
                if (adminId < 0) return;
                if ("/api/admin/auth/logout".equals(path)) {
                    accounts.logout(bearerToken(exchange), adminId);
                    respond(exchange, 200, Map.of("ok", true));
                    return;
                }
                if ("/api/admin/reports/status".equals(path)) {
                    new AdminRepository(databaseConfig).updateReport(
                            number(body, "reportId"), adminId, string(body, "status"));
                    respond(exchange, 200, Map.of("ok", true));
                    return;
                }
                if ("/api/admin/users/status".equals(path)) {
                    new AdminRepository(databaseConfig).updateUserStatus(
                            number(body, "userId"), string(body, "status"));
                    respond(exchange, 200, Map.of("ok", true));
                    return;
                }
                if ("/api/admin/users/create".equals(path)) {
                    long createdId = new AdminRepository(databaseConfig).createUser(
                            string(body, "displayName"), string(body, "username"),
                            string(body, "email"), string(body, "password"));
                    respond(exchange, 201, Map.of("userId", createdId));
                    return;
                }
                if ("/api/admin/users/update".equals(path)) {
                    new AdminRepository(databaseConfig).updateUser(
                            number(body, "userId"), string(body, "displayName"),
                            string(body, "username"), string(body, "email"));
                    respond(exchange, 200, Map.of("ok", true));
                    return;
                }
                respond(exchange, 404, Map.of("error", "Unknown API endpoint"));
                return;
            }
            long userId = authenticatedUser(exchange, accounts);
            if (userId < 0) return;
            if ("/api/auth/logout".equals(path)) {
                accounts.logout(bearerToken(exchange), userId);
                PRESENCE.offline(bearerToken(exchange));
                respond(exchange, 200, Map.of("ok", true));
                return;
            }
            if ("/api/auth/change-password".equals(path)) {
                accounts.changePassword(userId, string(body, "currentPassword"),
                        string(body, "newPassword"));
                respond(exchange, 200, Map.of("ok", true));
                return;
            }
            ChatRepository repository = new ChatRepository(databaseConfig, userId);
            switch (path) {
                case "/api/messages" -> respond(exchange, 200, Map.of("id",
                        repository.sendMessage(number(body, "conversationId"),
                                optionalNumber(body, "replyToId"), string(body, "content"))));
                case "/api/friend-requests" -> {
                    repository.requestFriend(number(body, "addresseeId"));
                    respond(exchange, 200, Map.of("ok", true));
                }
                case "/api/friend-requests/respond" -> {
                    repository.respondFriend(number(body, "friendshipId"),
                            Boolean.TRUE.equals(body.get("accept")));
                    respond(exchange, 200, Map.of("ok", true));
                }
                case "/api/groups" -> respond(exchange, 200, Map.of("id",
                        repository.createGroup(string(body, "name"), string(body, "description"),
                                Json.array(body.get("memberIds")).stream()
                                        .map(value -> ((Number) value).longValue()).toList())));
                case "/api/groups/update" -> {
                    repository.updateGroup(number(body, "conversationId"),
                            string(body, "name"), string(body, "description"));
                    respond(exchange, 200, Map.of("ok", true));
                }
                case "/api/groups/remove-member" -> {
                    repository.removeGroupMember(number(body, "conversationId"), number(body, "userId"));
                    respond(exchange, 200, Map.of("ok", true));
                }
                case "/api/groups/role" -> {
                    repository.changeGroupRole(number(body, "conversationId"), number(body, "userId"),
                            string(body, "role"));
                    respond(exchange, 200, Map.of("ok", true));
                }
                default -> respond(exchange, 404, Map.of("error", "Unknown API endpoint"));
            }
        } catch (AccountRepository.AuthenticationException error) {
            respond(exchange, 401, Map.of("error", error.getMessage()));
        } catch (IllegalArgumentException error) {
            respond(exchange, 400, Map.of("error", error.getMessage()));
        } catch (SQLException error) {
            System.err.println("Database error SQLSTATE " + error.getSQLState());
            respond(exchange, "23505".equals(error.getSQLState()) ? 409 : 500,
                    Map.of("error", "23505".equals(error.getSQLState())
                            ? "Email hoặc tên đăng nhập đã được sử dụng." : "Database operation failed"));
        } catch (RuntimeException error) {
            System.err.println("API error: " + error.getClass().getSimpleName());
            respond(exchange, 500, Map.of("error", "Server operation failed"));
        } finally {
            exchange.close();
        }
    }

    private static long authenticatedUser(HttpExchange exchange, AccountRepository accounts)
            throws IOException, SQLException {
        String token = bearerToken(exchange);
        long userId = token == null ? -1 : accounts.authenticatedUser(token);
        if (userId >= 0) PRESENCE.online(userId, token);
        if (userId < 0) respond(exchange, 401, Map.of("error", "Phiên đăng nhập không hợp lệ hoặc đã hết hạn."));
        return userId;
    }

    private static long authenticatedAdmin(HttpExchange exchange, AccountRepository accounts)
            throws IOException, SQLException {
        String token = bearerToken(exchange);
        long adminId = token == null ? -1 : accounts.authenticatedAdmin(token);
        if (adminId < 0) respond(exchange, 401, Map.of("error", "Cần đăng nhập bằng tài khoản quản trị."));
        return adminId;
    }

    private static String bearerToken(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    }

    private static long number(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (!(value instanceof Number number)) throw new IllegalArgumentException("Missing number: " + key);
        return number.longValue();
    }

    private static Long optionalNumber(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? null : number(body, key);
    }

    private static String string(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (!(value instanceof String text)) throw new IllegalArgumentException("Missing text: " + key);
        return text;
    }

    private static void respond(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = Json.stringify(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
