package com.javachat.shared.model;

public record MessageAttachment(
        long id,
        long messageId,
        String fileName,
        String fileUrl,
        String mimeType,
        long fileSize) {
}
