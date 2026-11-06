package com.javachat.shared.model;

import java.time.Instant;

/** Session metadata exposed to users/admins; token hashes are server-only. */
public record SessionInfo(
        long id,
        long userId,
        String ipAddress,
        String deviceInfo,
        Instant loggedInAt,
        Instant expiresAt,
        Instant loggedOutAt) {
}
