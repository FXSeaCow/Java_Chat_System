package com.javachat.shared.dto;

import java.util.List;

public final class CommonDtos {
    private CommonDtos() {
    }

    public record EmptyPayload() {
    }

    public record IdRequest(long id) {
    }

    public record AckResponse(boolean success) {
    }

    public record CursorRequest(String cursor, int limit) {
    }

    public record CursorPage<T>(List<T> items, String nextCursor, boolean hasMore) {
        public CursorPage {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record ErrorPayload(String code, String message, Object details) {
    }
}
