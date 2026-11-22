package com.javachat.user.net;

import com.javachat.shared.json.Json;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/** HTTP client for the loopback Java server. Database credentials never enter requests. */
public final class LiveApiClient {
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private final URI base;
    private volatile String token;

    public LiveApiClient() throws IOException {
        String value = System.getenv().getOrDefault("JAVA_CHAT_API_URL", "http://127.0.0.1:8080");
        base = URI.create(value);
        if (!"http".equals(base.getScheme()) || base.getHost() == null
                || !InetAddress.getByName(base.getHost()).isLoopbackAddress()) {
            throw new IllegalArgumentException("Development API URL must use HTTP on loopback");
        }
    }

    public Map<String, Object> snapshot() throws IOException {
        return request("GET", "/api/snapshot", null);
    }

    public Map<String, Object> post(String path, Map<String, Object> body) throws IOException {
        return request("POST", path, body);
    }

    public long login(String login, String password) throws IOException {
        Map<String, Object> result = post("/api/auth/login", Map.of(
                "login", login, "password", password));
        token = String.valueOf(result.get("token"));
        return ((Number) result.get("userId")).longValue();
    }

    public void logout() throws IOException {
        try {
            post("/api/auth/logout", Map.of());
        } finally {
            token = null;
        }
    }

    public void changePassword(String currentPassword, String newPassword) throws IOException {
        post("/api/auth/change-password", Map.of(
                "currentPassword", currentPassword, "newPassword", newPassword));
        token = null;
    }

    private Map<String, Object> request(String method, String path, Object body) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(base.resolve(path))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if ("POST".equals(method)) {
            builder.header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(Json.stringify(body), StandardCharsets.UTF_8));
        } else builder.GET();
        try {
            HttpResponse<String> response = client.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Map<String, Object> value = Json.object(Json.parse(response.body()));
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new IOException(String.valueOf(value.getOrDefault("error", "HTTP " + response.statusCode())));
            return value;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IOException("API request interrupted", error);
        } catch (IllegalArgumentException error) {
            throw new IOException("Invalid API response", error);
        }
    }
}
