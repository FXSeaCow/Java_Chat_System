package com.javachat.shared.protocol;

/**
 * Describes how a protocol message participates in the client-server exchange.
 */
public enum MessageKind {
    REQUEST,
    RESPONSE,
    EVENT,
    ERROR
}
