package com.javachat.shared.model;

import java.time.Instant;
import java.time.LocalDate;

/** Public user data. Authentication secrets never belong in this model. */
public record User(
        long id,
        String username,
        String email,
        String displayName,
        String avatarUrl,
        String address,
        LocalDate dateOfBirth,
        Gender gender,
        UserRole role,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
