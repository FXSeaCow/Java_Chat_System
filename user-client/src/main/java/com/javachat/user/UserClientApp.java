package com.javachat.user;

import com.javachat.shared.dto.ChatDtos.ConversationSummary;
import com.javachat.shared.dto.AiDtos;
import com.javachat.shared.dto.AuthDtos.PresenceChangedEvent;
import com.javachat.shared.dto.FriendDtos.FriendListResponse;
import com.javachat.shared.dto.FriendDtos.FriendRequestDecision;
import com.javachat.shared.model.ChatMessage;
import com.javachat.shared.model.ChatMessageStatus;
import com.javachat.shared.model.ChatMessageType;
import com.javachat.shared.model.Conversation;
import com.javachat.shared.model.ConversationType;
import com.javachat.shared.model.Friendship;
import com.javachat.shared.model.FriendshipStatus;
import com.javachat.shared.model.PresenceStatus;
import com.javachat.shared.model.User;
import com.javachat.shared.model.UserRole;
import com.javachat.shared.model.UserStatus;
import com.javachat.shared.model.GroupDetails;
import com.javachat.shared.model.ConversationMember;
import com.javachat.shared.model.MemberRole;
import com.javachat.user.ui.chat.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Set;

public final class UserClientApp {
    private UserClientApp() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            if (List.of(args).contains("--demo")) setupDemo(frame);
            frame.setVisible(true);
        });
    }

    private static void setupDemo(MainFrame frame) {
        Instant now = Instant.now();
        Map<Long, List<ChatMessage>> history = demoHistory(now);
        AtomicLong nextId = new AtomicLong(1000);
        List<ConversationSummary> conversations = new ArrayList<>(demoConversations(now));
        List<User> friends = new ArrayList<>(List.of(
                demoFriend(2, "minhanh", "Nguyễn Minh Anh", now),
                demoFriend(3, "giahuy", "Trần Gia Huy", now)));
        User incomingUser = demoFriend(4, "thao", "Lê Phương Thảo", now);
        User outgoingUser = demoFriend(5, "khanh", "Phạm Gia Khánh", now);
        List<Friendship> incoming = new ArrayList<>(List.of(new Friendship(10, 4, 1,
                FriendshipStatus.PENDING, now.minus(2, ChronoUnit.HOURS), null)));
        List<Friendship> outgoing = new ArrayList<>(List.of(new Friendship(11, 1, 5,
                FriendshipStatus.PENDING, now.minus(1, ChronoUnit.DAYS), null)));
        frame.setCurrentUserId(1L);
        frame.setConversations(conversations);
        frame.setFriendList(new FriendListResponse(friends, incoming, outgoing));
        frame.setFriendRequestUsers(List.of(incomingUser, outgoingUser));
        frame.setOnlineUserIds(List.of(2L));
        boolean[] huyOnline = {false};
        new Timer(10_000, event -> {
            huyOnline[0] = !huyOnline[0];
            frame.applyPresence(new PresenceChangedEvent(3,
                    huyOnline[0] ? PresenceStatus.ONLINE : PresenceStatus.OFFLINE, Instant.now()));
        }).start();
        frame.setRespondFriendRequestListener(request -> {
            if (incoming.removeIf(item -> item.id() == request.friendshipId())) {
                if (request.decision() == FriendRequestDecision.ACCEPT) {
                    friends.add(incomingUser);
                    Instant created = Instant.now();
                    conversations.add(new ConversationSummary(
                            new Conversation(4, ConversationType.DIRECT, 1L, created, created),
                            incomingUser.displayName(), null, null, 0));
                    frame.setConversations(conversations);
                }
                frame.setFriendList(new FriendListResponse(friends, incoming, outgoing));
            }
        });
        frame.setOpenFriendConversationListener(friend -> {
            long conversationId = friend.id() == 2 ? 1 : friend.id() == 3 ? 3 : 4;
            frame.openConversation(conversations.stream()
                    .filter(item -> item.conversation().id() == conversationId)
                    .findFirst().orElse(null));
        });
        frame.setPeople(demoPeople(now));
        frame.setGroupCandidates(demoPeople(now));
        frame.setGroups(List.of(new GroupDetails(2L, 1L, "Nhóm Java Chat", null,
                "Nhóm trao đổi đồ án Java", now.minus(12, ChronoUnit.DAYS))));
        frame.setGroupMembers(List.of(
                new ConversationMember(2L, 11L, MemberRole.OWNER, now.minus(12, ChronoUnit.DAYS), null, now),
                new ConversationMember(2L, 12L, MemberRole.ADMIN, now.minus(10, ChronoUnit.DAYS), null, now),
                new ConversationMember(2L, 13L, MemberRole.MEMBER, now.minus(8, ChronoUnit.DAYS), null, now)));
        frame.setFriendIds(Set.of(12L));
        frame.setPendingFriendIds(Set.of(13L));
        frame.setAddFriendListener(request -> frame.markFriendRequestSent(request.addresseeId()));
        frame.setOpenConversationListener(item -> frame.setMessages(item.conversation().id(),
                history.getOrDefault(item.conversation().id(), List.of())));
        frame.setSendMessageListener(request -> {
            ChatMessage message = new ChatMessage(nextId.incrementAndGet(), request.conversationId(), 1L,
                    request.replyToId(), request.content(), ChatMessageType.TEXT, ChatMessageStatus.SENT,
                    Instant.now(), null, List.of());
            history.computeIfAbsent(request.conversationId(), ignored -> new ArrayList<>()).add(message);
            frame.appendMessage(message);
        });
        frame.setAiSuggestionListener(request -> frame.setAiSuggestionResult(request.conversationId(),
                new AiDtos.ChatResponse(1L, "Mình đã xem tin nhắn của bạn. Mình sẽ phản hồi chi tiết ngay nhé!",
                        "Demo (câu trả lời mẫu)", 0, 0)));
    }

    private static List<User> demoPeople(Instant now) {
        return List.of(
                demoFriend(11L, "minhanh", "Nguyễn Minh Anh", now),
                demoFriend(12L, "giahuy", "Trần Gia Huy", now),
                demoFriend(13L, "thao.nguyen", "Nguyễn Thu Thảo", now),
                demoFriend(14L, "hoangnam", "Lê Hoàng Nam", now));
    }

    private static List<ConversationSummary> demoConversations(Instant now) {
        return List.of(
                sample(1, ConversationType.DIRECT, "Nguyễn Minh Anh", "Mai mình họp lúc 9 giờ nhé!", 2,
                        now.minus(7, ChronoUnit.MINUTES)),
                sample(2, ConversationType.GROUP, "Nhóm Java Chat", "Mình đã cập nhật giao diện.", 0,
                        now.minus(2, ChronoUnit.HOURS)),
                sample(3, ConversationType.DIRECT, "Trần Gia Huy", "Cảm ơn bạn nhiều", 1,
                        now.minus(1, ChronoUnit.DAYS)));
    }

    private static Map<Long, List<ChatMessage>> demoHistory(Instant now) {
        Map<Long, List<ChatMessage>> result = new HashMap<>();
        result.put(1L, new ArrayList<>(List.of(
                demoMessage(101, 1, 2, "Chào bạn! Phần giao diện chat đã sẵn sàng chưa?",
                        now.minus(34, ChronoUnit.MINUTES)),
                demoMessage(102, 1, 1, "Mình đang hoàn thiện khung chat và danh sách hội thoại.",
                        now.minus(28, ChronoUnit.MINUTES)),
                demoMessage(103, 1, 2, "Mai mình họp lúc 9 giờ nhé!",
                        now.minus(7, ChronoUnit.MINUTES)))));
        result.put(2L, new ArrayList<>(List.of(
                demoMessage(201, 2, 3, "Mọi người xem giúp bản thiết kế mới nhé.",
                        now.minus(5, ChronoUnit.HOURS)),
                demoMessage(202, 2, 1, "Mình đã cập nhật giao diện.",
                        now.minus(2, ChronoUnit.HOURS)))));
        result.put(3L, new ArrayList<>(List.of(
                demoMessage(301, 3, 1, "Mình đã gửi tài liệu cho bạn rồi.",
                        now.minus(1, ChronoUnit.DAYS).minus(12, ChronoUnit.MINUTES)),
                demoMessage(302, 3, 2, "Cảm ơn bạn nhiều", now.minus(1, ChronoUnit.DAYS)))));
        return result;
    }

    private static ConversationSummary sample(long id, ConversationType type, String title,
            String content, int unread, Instant at) {
        Conversation conversation = new Conversation(id, type, 1L, at, at);
        ChatMessage message = demoMessage(id * 100, id, 2, content, at);
        return new ConversationSummary(conversation, title, null, message, unread);
    }

    private static ChatMessage demoMessage(long id, long conversationId, long senderId, String content, Instant at) {
        return new ChatMessage(id, conversationId, senderId, null, content,
                ChatMessageType.TEXT, ChatMessageStatus.SENT, at, null, List.of());
    }

    private static User demoFriend(long id, String username, String displayName, Instant now) {
        return new User(id, username, username + "@example.com", displayName, null, null, null, null,
                UserRole.USER, UserStatus.ACTIVE, now, now);
    }
}
