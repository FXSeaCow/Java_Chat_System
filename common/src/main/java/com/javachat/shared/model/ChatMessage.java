package com.javachat.shared.model;

import java.time.Instant;
import java.util.List;

public record ChatMessage(
        long id,
        long conversationId,
        Long senderId,
        Long replyToId,
        String content,
        ChatMessageType messageType,
        ChatMessageStatus status,
        Instant createdAt,
        Instant editedAt,
        List<MessageAttachment> attachments) {

    public ChatMessage {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }
}
