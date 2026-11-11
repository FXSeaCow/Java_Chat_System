package com.javachat.shared.model;

import java.time.Instant;

public record GroupDetails(
        long conversationId,
        long ownerId,
        String name,
        String avatarUrl,
        String description,
        Instant createdAt) {
}
