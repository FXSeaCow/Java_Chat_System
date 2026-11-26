package com.javachat.server.net;

import com.javachat.server.database.AccountRepository;
import com.javachat.shared.json.Json;
import com.javachat.shared.model.PresenceStatus;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Run directly to verify the TCP listener without a database. */
public final class TcpServerSmokeTest {
    private TcpServerSmokeTest() { }

    public static void main(String[] args) throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        Set<String> validTokens = ConcurrentHashMap.newKeySet();
        TcpServer.Authentication authentication = new TcpServer.Authentication() {
            @Override
            public AccountRepository.LoginResult login(String login, String password,
                                                       String ipAddress, String deviceInfo) {
                if (!"user".equals(login) || !"secret".equals(password))
                    throw new AccountRepository.AuthenticationException();
                String token = UUID.randomUUID().toString();
                validTokens.add(token);
                return new AccountRepository.LoginResult(token, 7);
            }

            @Override
            public long authenticatedUser(String token) {
                return validTokens.contains(token) ? 7 : -1;
            }

            @Override
            public void logout(String token, long userId) {
                validTokens.remove(token);
            }
        };
        try (TcpServer server = new TcpServer(loopback, 0, authentication);
             Socket first = new Socket(loopback, server.port());
             Socket second = new Socket(loopback, server.port())) {
            first.setSoTimeout(2_000);
            second.setSoTimeout(2_000);
            UUID helloId = UUID.randomUUID();
            Map<String, Object> hello = exchange(first, Map.of("version", "1.0",
                    "type", "SYSTEM_HELLO_REQUEST", "requestId", helloId.toString(), "payload", Map.of()));
            require("SYSTEM_HELLO_RESPONSE".equals(hello.get("type")), "HELLO response missing");
            require(helloId.toString().equals(hello.get("requestId")), "Request ID changed");

            Map<String, Object> blocked = exchange(second, Map.of("version", "1.0",
                    "type", "SYSTEM_PING", "requestId", UUID.randomUUID().toString(), "payload", Map.of()));
            require("ERROR_RESPONSE".equals(blocked.get("type")), "Anonymous client was accepted");

            Map<String, Object> rejected = exchange(first, Map.of("version", "1.0",
                    "type", "AUTH_LOGIN_REQUEST", "requestId", UUID.randomUUID().toString(),
                    "payload", Map.of("login", "user", "password", "wrong")));
            require("ERROR_RESPONSE".equals(rejected.get("type")), "Wrong password was accepted");

            Map<String, Object> login = exchange(first, Map.of("version", "1.0",
                    "type", "AUTH_LOGIN_REQUEST", "requestId", UUID.randomUUID().toString(),
                    "payload", Map.of("login", "user", "password", "secret")));
            require("AUTH_LOGIN_RESPONSE".equals(login.get("type")), "Login failed");
            String token = (String) Json.object(login.get("payload")).get("token");

            Map<String, Object> resume = exchange(second, Map.of("version", "1.0",
                    "type", "SYSTEM_HELLO_REQUEST", "requestId", UUID.randomUUID().toString(),
                    "authToken", token, "payload", Map.of()));
            require(Boolean.TRUE.equals(Json.object(resume.get("payload")).get("authenticated")),
                    "Token did not authenticate second connection");

            Map<String, Object> pong = exchange(second, Map.of("version", "1.0",
                    "type", "SYSTEM_PING", "requestId", UUID.randomUUID().toString(), "payload", Map.of()));
            require("SYSTEM_PONG".equals(pong.get("type")), "Second client received no PONG");

            server.publishPresence(9L, PresenceStatus.ONLINE, Set.of(7L));
            require("USER_PRESENCE_CHANGED_EVENT".equals(read(first).get("type")),
                    "First client missed presence event");
            Map<String, Object> event = read(second);
            require("USER_PRESENCE_CHANGED_EVENT".equals(event.get("type")),
                    "Second client missed presence event");
            require(((Number) Json.object(event.get("payload")).get("userId")).longValue() == 9L,
                    "Presence event had the wrong user");

            Map<String, Object> logout = exchange(first, Map.of("version", "1.0",
                    "type", "AUTH_LOGOUT_REQUEST", "requestId", UUID.randomUUID().toString(), "payload", Map.of()));
            require("AUTH_LOGOUT_RESPONSE".equals(logout.get("type")), "Logout failed");
            Map<String, Object> expired = exchange(second, Map.of("version", "1.0",
                    "type", "SYSTEM_PING", "requestId", UUID.randomUUID().toString(), "payload", Map.of()));
            require("ERROR_RESPONSE".equals(expired.get("type")), "Revoked token remained valid");

            Map<String, Object> error = exchange(first, Map.of("version", "1.0",
                    "type", "MESSAGE_SEND_REQUEST", "requestId", UUID.randomUUID().toString(), "payload", Map.of()));
            require("ERROR_RESPONSE".equals(error.get("type")), "Unsupported request was accepted");
        }
    }

    private static Map<String, Object> exchange(Socket socket, Map<String, Object> request) throws Exception {
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        DataInputStream input = new DataInputStream(socket.getInputStream());
        byte[] bytes = Json.stringify(request).getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
        output.flush();
        return read(socket);
    }

    private static Map<String, Object> read(Socket socket) throws Exception {
        DataInputStream input = new DataInputStream(socket.getInputStream());
        int size = input.readInt();
        require(size > 0 && size <= 1_000_000, "Invalid response length");
        return Json.object(Json.parse(new String(input.readNBytes(size), StandardCharsets.UTF_8)));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
