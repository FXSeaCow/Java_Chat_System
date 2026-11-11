package com.javachat.shared.model;

import java.time.Instant;

public record AiRequest(
        long id,
        long userId,
        Long conversationId,
        String prompt,
        String response,
        String model,
        int inputTokens,
        int outputTokens,
        AiRequestStatus status,
        Instant createdAt) {
}
