package com.javachat.shared.dto;

import com.javachat.shared.model.ChatMessage;
import com.javachat.shared.model.ChatMessageType;
import com.javachat.shared.model.Conversation;
import com.javachat.shared.model.ReadReceipt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ChatDtos {
    private ChatDtos() {
    }

    public record ConversationListRequest(String cursor, int limit) {
    }

    public record ConversationListResponse(
            List<ConversationSummary> conversations,
            String nextCursor,
            boolean hasMore) {
        public ConversationListResponse {
            conversations = conversations == null ? List.of() : List.copyOf(conversations);
        }
    }

    public record ConversationSummary(
            Conversation conversation,
            String title,
            String avatarUrl,
            ChatMessage lastMessage,
            int unreadCount) {
    }

    public record ConversationGetRequest(long conversationId) {
    }

    public record DirectConversationCreateRequest(long otherUserId) {
    }

    public record MessageHistoryRequest(long conversationId, Long beforeMessageId, int limit) {
    }

    public record MessageHistoryResponse(
            List<ChatMessage> messages,
            Long nextBeforeMessageId,
            boolean hasMore) {
        public MessageHistoryResponse {
            messages = messages == null ? List.of() : List.copyOf(messages);
        }
    }

    public record AttachmentUpload(
            String fileName,
            String mimeType,
            long fileSize,
            String contentBase64) {
        @Override
        public String toString() {
            return "AttachmentUpload[fileName=" + fileName
                    + ", mimeType=" + mimeType
                    + ", fileSize=" + fileSize
                    + ", contentBase64=<omitted>]";
        }
    }

    public record MessageSendRequest(
            long conversationId,
            UUID clientMessageId,
            Long replyToId,
            ChatMessageType messageType,
            String content,
            List<AttachmentUpload> attachments) {
        public MessageSendRequest {
            attachments = attachments == null ? List.of() : List.copyOf(attachments);
        }
    }

    public record MessageSendResponse(long messageId, UUID clientMessageId, Instant createdAt) {
    }

    public record MessageEvent(ChatMessage message) {
    }

    public record MessageEditRequest(long messageId, String content) {
    }

    public record MessageDeleteRequest(long messageId) {
    }

    public record MessageReadRequest(long messageId) {
    }

    public record MessageReadEvent(ReadReceipt receipt) {
    }

    public record TypingEvent(long conversationId, long userId) {
    }
}
