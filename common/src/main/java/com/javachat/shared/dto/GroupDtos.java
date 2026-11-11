package com.javachat.shared.dto;

import com.javachat.shared.model.Conversation;
import com.javachat.shared.model.ConversationMember;
import com.javachat.shared.model.GroupDetails;
import com.javachat.shared.model.MemberRole;

import java.util.List;

public final class GroupDtos {
    private GroupDtos() {
    }

    public record GroupCreateRequest(
            String name,
            String description,
            String avatarUrl,
            List<Long> initialMemberIds) {
        public GroupCreateRequest {
            initialMemberIds = initialMemberIds == null ? List.of() : List.copyOf(initialMemberIds);
        }
    }

    public record GroupResponse(Conversation conversation, GroupDetails details) {
    }

    public record GroupUpdateRequest(
            long conversationId,
            String name,
            String description,
            String avatarUrl) {
    }

    public record GroupIdRequest(long conversationId) {
    }

    public record MemberListResponse(List<ConversationMember> members) {
        public MemberListResponse {
            members = members == null ? List.of() : List.copyOf(members);
        }
    }

    public record MemberAddRequest(long conversationId, long userId) {
    }

    public record MemberRemoveRequest(long conversationId, long userId) {
    }

    public record MemberRoleUpdateRequest(long conversationId, long userId, MemberRole role) {
    }

    public record MemberEvent(long conversationId, ConversationMember member) {
    }
}
