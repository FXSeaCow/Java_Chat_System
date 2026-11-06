package com.javachat.shared.dto;

import com.javachat.shared.model.Friendship;
import com.javachat.shared.model.User;

import java.util.List;

public final class FriendDtos {
    private FriendDtos() {
    }

    public enum FriendRequestDecision {
        ACCEPT,
        REJECT
    }

    public record FriendListResponse(
            List<User> friends,
            List<Friendship> incomingRequests,
            List<Friendship> outgoingRequests) {
        public FriendListResponse {
            friends = friends == null ? List.of() : List.copyOf(friends);
            incomingRequests = incomingRequests == null ? List.of() : List.copyOf(incomingRequests);
            outgoingRequests = outgoingRequests == null ? List.of() : List.copyOf(outgoingRequests);
        }
    }

    public record SendRequest(long addresseeId) {
    }

    public record RespondRequest(long friendshipId, FriendRequestDecision decision) {
    }

    public record FriendshipResponse(Friendship friendship) {
    }

    public record FriendshipEvent(Friendship friendship, User actor) {
    }

    public record UserRelationRequest(long userId) {
    }
}
