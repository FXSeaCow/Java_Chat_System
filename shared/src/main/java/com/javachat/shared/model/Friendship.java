package com.javachat.shared.model;

import java.time.Instant;

public record Friendship(
        long id,
        long requesterId,
        long addresseeId,
        FriendshipStatus status,
        Instant createdAt,
        Instant respondedAt) {
}
