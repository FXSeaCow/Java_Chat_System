package com.javachat.user.net;

import com.javachat.user.mock.MockChatStore;

import java.util.Map;
import java.util.List;

/** Destructive integration check: run only against a disposable seeded database. */
public final class LiveApiWriteSmokeTest {
    private LiveApiWriteSmokeTest() { }

    public static void main(String[] args) throws Exception {
        LiveApiClient api = new LiveApiClient();
        var sent = api.post("/api/messages",
                Map.of("conversationId", 1L, "content", "Integration test message"));
        long messageId = ((Number) sent.get("id")).longValue();
        if (messageId != 13L) throw new AssertionError("Message insert did not use seed sequence");

        var created = api.post("/api/groups",
                Map.of("name", "Integration group", "description", "Temporary", "memberIds", List.of(2L)));
        long groupId = ((Number) created.get("id")).longValue();
        if (groupId != 6L) throw new AssertionError("Group insert did not use seed sequence");
        api.post("/api/groups/update", Map.of(
                "conversationId", groupId, "name", "Updated integration group", "description", "Temporary"));
        api.post("/api/groups/role", Map.of(
                "conversationId", groupId, "userId", 2L, "role", "ADMIN"));
        api.post("/api/groups/remove-member", Map.of("conversationId", groupId, "userId", 2L));
        api.post("/api/friend-requests", Map.of("addresseeId", 4L));

        MockChatStore snapshot = MockChatStore.fromSnapshot(api.snapshot());
        if (snapshot.messages(1L).size() != 3
                || snapshot.myGroups().stream().noneMatch(group ->
                        group.conversationId() == groupId && group.name().equals("Updated integration group"))
                || snapshot.groupMembers(groupId).size() != 1
                || snapshot.friendList().outgoingRequests().stream()
                        .noneMatch(request -> request.addresseeId() == 4L)) {
            throw new AssertionError("Writes were not reflected in the database snapshot");
        }
        System.out.println("LiveApiWriteSmokeTest passed");
    }
}
