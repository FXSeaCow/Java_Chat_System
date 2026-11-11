package com.javachat.shared.model;

import java.time.Instant;

public record ReadReceipt(long messageId, long userId, Instant readAt) {
}
