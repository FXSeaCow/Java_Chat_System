package com.javachat.shared.model;

import java.time.Instant;

/** Public user data. Authentication secrets never belong in this model. */
public record User(
        long id,
        String username,
        String email,
        String displayName,
        String avatarUrl,
        UserRole role,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
