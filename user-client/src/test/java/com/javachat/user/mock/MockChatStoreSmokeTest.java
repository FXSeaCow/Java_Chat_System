package com.javachat.user.mock;

import com.javachat.shared.dto.ChatDtos.MessageSendRequest;
import com.javachat.shared.dto.GroupDtos.GroupCreateRequest;
import com.javachat.shared.model.ChatMessageType;
import com.javachat.shared.model.MemberRole;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MockChatStoreSmokeTest {
    private MockChatStoreSmokeTest() { }

    public static void main(String[] args) {
        MockChatStore store = new MockChatStore();
        check(store.allUsers().size() == 8, "seed users");
        check(store.friendList().friends().stream().map(user -> user.id()).toList().equals(List.of(2L, 3L)),
                "seed friendships");
        MockChatStore live = MockChatStore.fromSnapshot(Map.of(
                "currentUserId", 1L, "onlineFriendIds", List.of(2L, 3L),
                "users", List.of(), "friendships", List.of(), "conversations", List.of(),
                "members", List.of(), "groups", List.of(), "messages", List.of(),
                "attachments", List.of()));
        check(live.friendList().onlineFriendIds().equals(List.of(2L, 3L)),
                "online friend IDs from server snapshot");
        check(store.conversationSummaries().stream().map(item -> item.conversation().id()).toList()
                .equals(List.of(4L, 1L, 2L)), "current user's conversations");
        check(store.directConversationWith(3L).conversation().id() == 2L, "direct conversation mapping");
        check(store.myGroups().size() == 1 && store.myGroups().get(0).conversationId() == 4L,
                "group mapping");
        check(store.groupMembers(4L).size() == 5, "active group members");
        check(store.groupMembers(4L).get(0).role() == MemberRole.OWNER, "group owner");
        check(store.messages(4L).stream().map(message -> message.id()).toList()
                .equals(List.of(9L, 10L, 6L, 7L, 8L)), "chronological message order");
        check(store.messages(4L).stream().filter(message -> message.id() == 7L)
                .findFirst().orElseThrow().attachments().size() == 1, "seed attachment");

        var sent = store.send(new MessageSendRequest(1L, UUID.randomUUID(), null,
                ChatMessageType.TEXT, "Tin nhắn thử", List.of()));
        check(sent.id() == 13L && store.messages(1L).size() == 3, "in-memory message send");
        check(store.conversationSummaries().get(0).conversation().id() == 1L,
                "conversation reordered after send");
        var newGroup = store.createGroup(new GroupCreateRequest("Nhóm mới", "Mô tả", null, List.of(2L)));
        check(newGroup.conversation().id() == 6L && store.groupMembers(6L).size() == 2,
                "in-memory group creation");
        System.out.println("MockChatStoreSmokeTest passed");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
