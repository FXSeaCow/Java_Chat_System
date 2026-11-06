package com.javachat.shared.dto;

import java.time.Instant;
import java.util.List;

public final class SystemDtos {
    private SystemDtos() {
    }

    public record HelloRequest(String clientName, String clientVersion, String protocolVersion) {
    }

    public record HelloResponse(
            String serverVersion,
            String protocolVersion,
            List<String> capabilities) {
        public HelloResponse {
            capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
        }
    }

    public record PingPayload(Instant sentAt) {
    }

    public record PongPayload(Instant sentAt, Instant receivedAt) {
    }
}
