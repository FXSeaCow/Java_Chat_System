package com.javachat.server;

import com.javachat.server.database.ChatRepository;
import com.javachat.server.database.DatabaseConfig;
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
import java.util.concurrent.Executors;

/** Loopback-only API for the desktop client while account authentication is pending. */
public final class ServerApp {
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
        int port = Integer.parseInt(settings.getProperty("server.port", "8080").trim());
        long userId = Long.parseLong(settings.getProperty("server.user_id", "1").trim());
        InetAddress address = InetAddress.getByName(host);
        if (!address.isLoopbackAddress()) {
            throw new IllegalArgumentException("Unauthenticated development API must bind to loopback");
        }
        ChatRepository repository = new ChatRepository(DatabaseConfig.load(), userId);
        HttpServer server = HttpServer.create(new InetSocketAddress(address, port), 0);
        server.createContext("/api/", exchange -> handle(exchange, repository));
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        System.out.println("E-Chat API listening at http://" + host + ":" + server.getAddress().getPort());
    }

    private static void handle(HttpExchange exchange, ChatRepository repository) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if ("GET".equals(method) && "/api/snapshot".equals(path)) {
                respond(exchange, 200, repository.snapshot());
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
        } catch (IllegalArgumentException error) {
            respond(exchange, 400, Map.of("error", error.getMessage()));
        } catch (SQLException error) {
            System.err.println("Database error SQLSTATE " + error.getSQLState());
            respond(exchange, "23505".equals(error.getSQLState()) ? 409 : 500,
                    Map.of("error", "Database operation failed"));
        } catch (RuntimeException error) {
            System.err.println("API error: " + error.getClass().getSimpleName());
            respond(exchange, 500, Map.of("error", "Server operation failed"));
        } finally {
            exchange.close();
        }
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
