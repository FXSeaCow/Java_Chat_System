package com.javachat.admin.net;

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

/** Role-checked loopback API client; database credentials remain on the server. */
public final class AdminApiClient {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final URI base;
    private String token;

    public AdminApiClient() throws IOException {
        base = URI.create(System.getenv().getOrDefault("JAVA_CHAT_API_URL", "http://127.0.0.1:8080"));
        if (!"http".equals(base.getScheme()) || base.getHost() == null
                || !InetAddress.getByName(base.getHost()).isLoopbackAddress())
            throw new IOException("API quản trị chỉ được chạy trên địa chỉ loopback HTTP.");
    }

    public void login(String username, String password) throws IOException {
        Map<String, Object> result;
        try {
            result = request("POST", "/api/admin/auth/login",
                    Map.of("login", username, "password", password));
        } catch (IOException error) {
            if (error.getMessage() != null && error.getMessage().contains("Unknown API endpoint"))
                throw new IOException("Server đang chạy bản cũ. Hãy khởi động lại run-server.ps1.", error);
            throw error;
        }
        token = String.valueOf(result.get("token"));
    }

    public Map<String, Object> snapshot() throws IOException {
        return request("GET", "/api/admin/snapshot", null);
    }

    public void updateReport(long reportId, String status) throws IOException {
        request("POST", "/api/admin/reports/status", Map.of("reportId", reportId, "status", status));
    }

    public void updateUserStatus(long userId, String status) throws IOException {
        request("POST", "/api/admin/users/status", Map.of("userId", userId, "status", status));
    }

    public void logout() throws IOException {
        try { request("POST", "/api/admin/auth/logout", Map.of()); }
        finally { token = null; }
    }

    private Map<String, Object> request(String method, String path, Object body) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(base.resolve(path))
                .timeout(Duration.ofSeconds(20)).header("Accept", "application/json")
                .header("User-Agent", "E-Chat Admin Desktop");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if ("POST".equals(method)) builder.header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(Json.stringify(body), StandardCharsets.UTF_8));
        else builder.GET();
        try {
            HttpResponse<String> response = client.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Map<String, Object> value = Json.object(Json.parse(response.body()));
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new IOException(String.valueOf(value.getOrDefault("error", "HTTP " + response.statusCode())));
            return value;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IOException("Yêu cầu API bị ngắt.", error);
        } catch (IllegalArgumentException error) {
            throw new IOException("Phản hồi API không hợp lệ.", error);
        }
    }
}
