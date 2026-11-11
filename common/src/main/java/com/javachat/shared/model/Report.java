package com.javachat.shared.model;

import java.time.Instant;

public record Report(
        long id,
        long reporterId,
        long messageId,
        String reason,
        String details,
        ReportStatus status,
        Long reviewedBy,
        Instant createdAt,
        Instant reviewedAt) {
}
