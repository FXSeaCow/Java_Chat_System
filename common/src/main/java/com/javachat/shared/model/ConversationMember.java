package com.javachat.shared.model;

import java.time.Instant;

public record ConversationMember(
        long conversationId,
        long userId,
        MemberRole role,
        Instant joinedAt,
        Instant leftAt,
        Instant lastReadAt) {
}
