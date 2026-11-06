package com.javachat.shared.dto;

public final class AiDtos {
    private AiDtos() {
    }

    public record ChatRequest(Long conversationId, String prompt) {
    }

    public record ChatResponse(
            long requestId,
            String response,
            String model,
            int inputTokens,
            int outputTokens) {
    }
}
