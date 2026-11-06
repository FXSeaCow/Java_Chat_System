package com.javachat.shared.dto;

import com.javachat.shared.protocol.MessageKind;
import com.javachat.shared.protocol.MessageType;

import java.util.UUID;

/** Dependency-free smoke test; executable with plain javac/java. */
public final class ProtocolMessageSmokeTest {
    private ProtocolMessageSmokeTest() {
    }

    public static void main(String[] args) {
        var credentials = new AuthDtos.LoginRequest("an.nguyen", "super-secret-123", "JavaFX");
        var request = ProtocolMessage.request(
                MessageType.AUTH_LOGIN_REQUEST,
                UUID.randomUUID(),
                null,
                credentials);

        require(request.type().getKind() == MessageKind.REQUEST, "Wrong message kind");
        require(!request.toString().contains("super-secret-123"), "Password leaked into log output");

        var typing = ProtocolMessage.event(
                MessageType.TYPING_STARTED_EVENT,
                new ChatDtos.TypingEvent(4L, 1L));
        require(typing.requestId() == null, "Events must allow a null requestId");

        boolean rejected = false;
        try {
            ProtocolMessage.request(
                    MessageType.MESSAGE_CREATED_EVENT,
                    UUID.randomUUID(),
                    null,
                    new ChatDtos.TypingEvent(4L, 1L));
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        require(rejected, "Request factory accepted an event type");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
