package com.javachat.shared.dto;

import com.javachat.shared.protocol.MessageKind;
import com.javachat.shared.protocol.MessageType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** JSON envelope shared by every protocol message. */
public record ProtocolMessage<T>(
        String version,
        MessageType type,
        UUID requestId,
        Instant timestamp,
        String authToken,
        T payload) {

    public static final String CURRENT_VERSION = "1.0";

    public ProtocolMessage {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(timestamp, "timestamp");

        if (type.getKind() != MessageKind.EVENT && requestId == null) {
            throw new IllegalArgumentException("requestId is required for non-event messages");
        }
    }

    public static <T> ProtocolMessage<T> request(
            MessageType type, UUID requestId, String authToken, T payload) {
        if (type.getKind() != MessageKind.REQUEST) {
            throw new IllegalArgumentException("Expected a request message type");
        }
        return new ProtocolMessage<>(CURRENT_VERSION, type, requestId, Instant.now(), authToken, payload);
    }

    public static <T> ProtocolMessage<T> response(
            MessageType type, UUID requestId, T payload) {
        if (type.getKind() != MessageKind.RESPONSE) {
            throw new IllegalArgumentException("Expected a response message type");
        }
        return new ProtocolMessage<>(CURRENT_VERSION, type, requestId, Instant.now(), null, payload);
    }

    public static <T> ProtocolMessage<T> event(MessageType type, T payload) {
        if (type.getKind() != MessageKind.EVENT) {
            throw new IllegalArgumentException("Expected an event message type");
        }
        return new ProtocolMessage<>(CURRENT_VERSION, type, null, Instant.now(), null, payload);
    }

    /** Prevents session tokens from leaking through routine logging. */
    @Override
    public String toString() {
        return "ProtocolMessage[version=" + version
                + ", type=" + type
                + ", requestId=" + requestId
                + ", timestamp=" + timestamp
                + ", authToken=" + (authToken == null ? "null" : "<redacted>")
                + ", payload=" + payload + "]";
    }
}
