package com.javachat.shared.dto;

import com.javachat.shared.model.Report;
import com.javachat.shared.model.ReportStatus;
import com.javachat.shared.model.SessionInfo;
import com.javachat.shared.model.UserStatus;

import java.time.Instant;
import java.util.List;

public final class AdminDtos {
    private AdminDtos() {
    }

    public enum ReportDecision {
        REVIEW,
        DISMISS
    }

    public record ReportCreateRequest(long messageId, String reason, String details) {
    }

    public record ReportListRequest(ReportStatus status, String cursor, int limit) {
    }

    public record ReportListResponse(List<Report> reports, String nextCursor, boolean hasMore) {
        public ReportListResponse {
            reports = reports == null ? List.of() : List.copyOf(reports);
        }
    }

    public record ReportReviewRequest(long reportId, ReportDecision decision) {
    }

    public record UserStatusUpdateRequest(long userId, UserStatus status) {
    }

    public record LoginHistoryRequest(long userId, Instant from, Instant to, int limit) {
    }

    public record LoginHistoryResponse(List<SessionInfo> sessions) {
        public LoginHistoryResponse {
            sessions = sessions == null ? List.of() : List.copyOf(sessions);
        }
    }

    public record StatsResponse(
            long totalUsers,
            long activeUsers,
            long blockedUsers,
            long totalGroups,
            long totalMessages,
            long pendingReports) {
    }
}
