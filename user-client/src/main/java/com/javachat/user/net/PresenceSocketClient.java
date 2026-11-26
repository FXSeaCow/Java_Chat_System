package com.javachat.user.net;

import com.javachat.shared.dto.AuthDtos.PresenceChangedEvent;
import com.javachat.shared.json.Json;
import com.javachat.shared.model.PresenceStatus;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Receives server-pushed presence events over the project's TCP protocol. */
public final class PresenceSocketClient implements AutoCloseable {
    private final String host;
    private final int port;
    private final String token;
    private final Consumer<PresenceChangedEvent> onEvent;
    private final Thread worker;
    private volatile boolean running = true;
    private volatile Socket socket;

    public PresenceSocketClient(String host, int port, String token,
                                Consumer<PresenceChangedEvent> onEvent) {
        this.host = host;
        this.port = port;
        this.token = token;
        this.onEvent = onEvent;
        worker = Thread.ofVirtual().name("chat-presence-events").start(this::run);
    }

    private void run() {
        while (running) {
            try (Socket connection = new Socket()) {
                socket = connection;
                connection.connect(new InetSocketAddress(host, port), 5_000);
                connection.setSoTimeout(15_000);
                DataInputStream input = new DataInputStream(connection.getInputStream());
                DataOutputStream output = new DataOutputStream(connection.getOutputStream());
                send(output, "SYSTEM_HELLO_REQUEST", token);
                Map<String, Object> hello = read(input);
                if (!"SYSTEM_HELLO_RESPONSE".equals(hello.get("type"))
                        || !Boolean.TRUE.equals(Json.object(hello.get("payload")).get("authenticated"))) return;
                while (running) {
                    try {
                        Map<String, Object> message = read(input);
                        if ("USER_PRESENCE_CHANGED_EVENT".equals(message.get("type"))) {
                            Map<String, Object> payload = Json.object(message.get("payload"));
                            onEvent.accept(new PresenceChangedEvent(
                                    ((Number) payload.get("userId")).longValue(),
                                    PresenceStatus.valueOf((String) payload.get("status")),
                                    Instant.parse((String) payload.get("changedAt"))));
                        }
                    } catch (SocketTimeoutException idle) {
                        send(output, "SYSTEM_PING", null);
                    }
                }
            } catch (IOException | RuntimeException error) {
                if (!running) return;
            } finally {
                socket = null;
            }
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static void send(DataOutputStream output, String type, String authToken) throws IOException {
        Map<String, Object> message = new java.util.LinkedHashMap<>();
        message.put("version", "1.0");
        message.put("type", type);
        message.put("requestId", UUID.randomUUID().toString());
        message.put("timestamp", Instant.now().toString());
        message.put("authToken", authToken);
        message.put("payload", Map.of());
        byte[] bytes = Json.stringify(message).getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
        output.flush();
    }

    private static Map<String, Object> read(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length <= 0 || length > 1_000_000) throw new IOException("Invalid presence frame");
        return Json.object(Json.parse(new String(input.readNBytes(length), StandardCharsets.UTF_8)));
    }

    @Override
    public void close() {
        running = false;
        Socket current = socket;
        if (current != null) {
            try { current.close(); } catch (IOException ignored) { }
        }
        worker.interrupt();
    }
}
