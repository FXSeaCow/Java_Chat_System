package com.javachat.shared.dto;

import com.javachat.shared.model.PresenceStatus;
import com.javachat.shared.model.User;

import java.time.Instant;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(
            String username,
            String email,
            String password,
            String displayName) {
        @Override
        public String toString() {
            return "RegisterRequest[username=" + username + ", email=" + email
                    + ", password=<redacted>, displayName=" + displayName + "]";
        }
    }

    public record LoginRequest(String login, String password, String deviceInfo) {
        @Override
        public String toString() {
            return "LoginRequest[login=" + login
                    + ", password=<redacted>, deviceInfo=" + deviceInfo + "]";
        }
    }

    public record AuthResponse(String token, Instant expiresAt, User user) {
    }

    public record ProfileGetRequest(long userId) {
    }

    public record ProfileUpdateRequest(String displayName, String avatarUrl) {
    }

    public record SessionExpiredEvent(String reason) {
    }

    public record PresenceChangedEvent(long userId, PresenceStatus status, Instant changedAt) {
    }
}
