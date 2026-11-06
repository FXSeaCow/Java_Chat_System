package com.javachat.shared.model;

import java.time.Instant;

public record Conversation(
        long id,
        ConversationType type,
        Long createdBy,
        Instant createdAt,
        Instant updatedAt) {
}
