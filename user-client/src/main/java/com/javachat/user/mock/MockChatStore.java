package com.javachat.user.mock;

import com.javachat.shared.dto.ChatDtos.ConversationSummary;
import com.javachat.shared.dto.ChatDtos.MessageSendRequest;
import com.javachat.shared.dto.FriendDtos.FriendListResponse;
import com.javachat.shared.dto.GroupDtos.GroupCreateRequest;
import com.javachat.shared.dto.GroupDtos.GroupUpdateRequest;
import com.javachat.shared.json.Json;
import com.javachat.shared.model.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * In-memory snapshot of database/seed.sql for UI previews.
 * IDs, relationships, timestamps, and message content follow the seed data.
 * Changes made in the preview live only until the application closes.
 */
public final class MockChatStore {
    public static final long CURRENT_USER_ID = 1L;
    private static final ZoneId SEED_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final Map<Long, User> users = new LinkedHashMap<>();
    private final Map<Long, Conversation> conversations = new LinkedHashMap<>();
    private final Map<Long, GroupDetails> groups = new LinkedHashMap<>();
    private final Map<Long, List<ChatMessage>> messages = new HashMap<>();
    private final List<ConversationMember> members = new ArrayList<>();
    private final List<Friendship> friendships = new ArrayList<>();
    private long nextConversationId = 6;
    private long nextMessageId = 13;
    private long nextFriendshipId = 7;

    public MockChatStore() {
        seedUsers();
        seedFriendships();
        seedConversations();
        seedMembers();
        seedGroups();
        seedMessages();
    }

    /** Builds the same view model from rows returned by the server API. */
    public static MockChatStore fromSnapshot(Map<String, Object> snapshot) {
        if (number(snapshot, "currentUserId") != CURRENT_USER_ID)
            throw new IllegalArgumentException("This desktop preview expects user ID 1");
        MockChatStore store = new MockChatStore(false);
        for (Object value : Json.array(snapshot.get("users"))) {
            Map<String, Object> row = Json.object(value);
            long id = number(row, "id");
            store.users.put(id, new User(id, text(row, "username"), text(row, "email"),
                    text(row, "display_name"), text(row, "avatar_url"), text(row, "address"),
                    date(row, "date_of_birth"), enumValue(Gender.class, row, "gender"),
                    enumValue(UserRole.class, row, "role"), enumValue(UserStatus.class, row, "status"),
                    instant(row, "created_at"), instant(row, "updated_at")));
        }
        for (Object value : Json.array(snapshot.get("friendships"))) {
            Map<String, Object> row = Json.object(value);
            long id = number(row, "id");
            store.friendships.add(new Friendship(id, number(row, "requester_id"),
                    number(row, "addressee_id"), enumValue(FriendshipStatus.class, row, "status"),
                    instant(row, "created_at"), instant(row, "responded_at")));
            store.nextFriendshipId = Math.max(store.nextFriendshipId, id + 1);
        }
        for (Object value : Json.array(snapshot.get("conversations"))) {
            Map<String, Object> row = Json.object(value);
            long id = number(row, "id");
            store.conversations.put(id, new Conversation(id, enumValue(ConversationType.class, row, "type"),
                    optionalNumber(row, "created_by"), instant(row, "created_at"),
                    instant(row, "updated_at")));
            store.nextConversationId = Math.max(store.nextConversationId, id + 1);
        }
        for (Object value : Json.array(snapshot.get("members"))) {
            Map<String, Object> row = Json.object(value);
            store.members.add(new ConversationMember(number(row, "conversation_id"),
                    number(row, "user_id"), enumValue(MemberRole.class, row, "member_role"),
                    instant(row, "joined_at"), instant(row, "left_at"), instant(row, "last_read_at")));
        }
        for (Object value : Json.array(snapshot.get("groups"))) {
            Map<String, Object> row = Json.object(value);
            long id = number(row, "conversation_id");
            store.groups.put(id, new GroupDetails(id, number(row, "owner_id"), text(row, "name"),
                    text(row, "avatar_url"), text(row, "description"), instant(row, "created_at")));
        }
        Map<Long, List<MessageAttachment>> attachments = new HashMap<>();
        for (Object value : Json.array(snapshot.get("attachments"))) {
            Map<String, Object> row = Json.object(value);
            long messageId = number(row, "message_id");
            attachments.computeIfAbsent(messageId, ignored -> new ArrayList<>())
                    .add(new MessageAttachment(number(row, "id"), messageId,
                            text(row, "file_name"), text(row, "file_url"),
                            text(row, "mime_type"), number(row, "file_size")));
        }
        for (Object value : Json.array(snapshot.get("messages"))) {
            Map<String, Object> row = Json.object(value);
            long id = number(row, "id");
            long conversationId = number(row, "conversation_id");
            store.messages.computeIfAbsent(conversationId, ignored -> new ArrayList<>())
                    .add(new ChatMessage(id, conversationId, optionalNumber(row, "sender_id"),
                            optionalNumber(row, "reply_to_id"), text(row, "content"),
                            enumValue(ChatMessageType.class, row, "message_type"),
                            enumValue(ChatMessageStatus.class, row, "status"),
                            instant(row, "created_at"), instant(row, "edited_at"),
                            attachments.getOrDefault(id, List.of())));
            store.nextMessageId = Math.max(store.nextMessageId, id + 1);
        }
        store.messages.values().forEach(items -> items.sort(Comparator.comparing(ChatMessage::createdAt)));
        return store;
    }

    private MockChatStore(boolean seed) {
        if (seed) {
            seedUsers();
            seedFriendships();
            seedConversations();
            seedMembers();
            seedGroups();
            seedMessages();
        }
    }

    private static long number(Map<String, Object> row, String key) {
        return ((Number) row.get(key)).longValue();
    }

    private static Long optionalNumber(Map<String, Object> row, String key) {
        return row.get(key) == null ? null : number(row, key);
    }

    private static String text(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? null : value.toString();
    }

    private static Instant instant(Map<String, Object> row, String key) {
        String value = text(row, key);
        return value == null ? null : Instant.parse(value);
    }

    private static LocalDate date(Map<String, Object> row, String key) {
        String value = text(row, key);
        return value == null ? null : LocalDate.parse(value);
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, Map<String, Object> row, String key) {
        String value = text(row, key);
        return value == null ? null : Enum.valueOf(type, value);
    }

    public List<User> people() {
        return users.values().stream()
                .filter(user -> user.id() != CURRENT_USER_ID && user.role() == UserRole.USER
                        && user.status() == UserStatus.ACTIVE)
                .toList();
    }

    public List<User> allUsers() {
        return List.copyOf(users.values());
    }

    public FriendListResponse friendList() {
        List<User> friends = friendships.stream()
                .filter(item -> item.status() == FriendshipStatus.ACCEPTED && isMine(item))
                .map(item -> users.get(otherUserId(item))).toList();
        List<Friendship> incoming = friendships.stream()
                .filter(item -> item.status() == FriendshipStatus.PENDING
                        && item.addresseeId() == CURRENT_USER_ID).toList();
        List<Friendship> outgoing = friendships.stream()
                .filter(item -> item.status() == FriendshipStatus.PENDING
                        && item.requesterId() == CURRENT_USER_ID).toList();
        return new FriendListResponse(friends, incoming, outgoing);
    }

    public List<User> requestUsers() {
        return friendships.stream()
                .filter(item -> item.status() == FriendshipStatus.PENDING && isMine(item))
                .map(item -> users.get(otherUserId(item))).toList();
    }

    public Set<Long> friendIds() {
        return friendList().friends().stream().map(User::id).collect(Collectors.toSet());
    }

    public Set<Long> pendingFriendIds() {
        return friendList().outgoingRequests().stream()
                .map(Friendship::addresseeId).collect(Collectors.toSet());
    }

    public void sendFriendRequest(long addresseeId) {
        if (!users.containsKey(addresseeId) || friendIds().contains(addresseeId)
                || pendingFriendIds().contains(addresseeId)) return;
        friendships.add(new Friendship(nextFriendshipId++, CURRENT_USER_ID, addresseeId,
                FriendshipStatus.PENDING, Instant.now(), null));
    }

    public void respondToFriendRequest(long friendshipId, boolean accept) {
        for (int i = 0; i < friendships.size(); i++) {
            Friendship item = friendships.get(i);
            if (item.id() == friendshipId && item.addresseeId() == CURRENT_USER_ID
                    && item.status() == FriendshipStatus.PENDING) {
                friendships.remove(i);
                if (accept) {
                    friendships.add(new Friendship(item.id(), item.requesterId(), item.addresseeId(),
                            FriendshipStatus.ACCEPTED, item.createdAt(), Instant.now()));
                    createDirectConversation(item.requesterId());
                }
                return;
            }
        }
    }

    public List<ConversationSummary> conversationSummaries() {
        return conversations.values().stream()
                .filter(conversation -> activeMember(conversation.id(), CURRENT_USER_ID) != null)
                .map(this::summary)
                .sorted(Comparator.comparing((ConversationSummary item) ->
                        item.conversation().updatedAt()).reversed())
                .toList();
    }

    public ConversationSummary directConversationWith(long userId) {
        return conversations.values().stream()
                .filter(item -> item.type() == ConversationType.DIRECT
                        && activeMember(item.id(), CURRENT_USER_ID) != null
                        && activeMember(item.id(), userId) != null)
                .findFirst().map(this::summary).orElse(null);
    }

    public List<ChatMessage> messages(long conversationId) {
        return List.copyOf(messages.getOrDefault(conversationId, List.of()));
    }

    public ChatMessage send(MessageSendRequest request) {
        if (activeMember(request.conversationId(), CURRENT_USER_ID) == null) {
            throw new IllegalArgumentException("Not a member of conversation " + request.conversationId());
        }
        Instant sentAt = Instant.now();
        ChatMessage message = new ChatMessage(nextMessageId++, request.conversationId(), CURRENT_USER_ID,
                request.replyToId(), request.content(), request.messageType(), ChatMessageStatus.SENT,
                sentAt, null, List.of());
        messages.computeIfAbsent(request.conversationId(), ignored -> new ArrayList<>()).add(message);
        Conversation old = conversations.get(request.conversationId());
        conversations.put(old.id(), new Conversation(old.id(), old.type(), old.createdBy(),
                old.createdAt(), sentAt));
        return message;
    }

    public List<GroupDetails> myGroups() {
        return groups.values().stream()
                .filter(group -> activeMember(group.conversationId(), CURRENT_USER_ID) != null)
                .toList();
    }

    public List<ConversationMember> groupMembers(long conversationId) {
        return members.stream().filter(item -> item.conversationId() == conversationId
                && item.leftAt() == null).toList();
    }

    public void updateGroup(GroupUpdateRequest request) {
        GroupDetails old = groups.get(request.conversationId());
        if (old == null || old.ownerId() != CURRENT_USER_ID) return;
        groups.put(old.conversationId(), new GroupDetails(old.conversationId(), old.ownerId(),
                request.name(), request.avatarUrl(), request.description(), old.createdAt()));
    }

    public ConversationSummary createGroup(GroupCreateRequest request) {
        long id = nextConversationId++;
        Instant now = Instant.now();
        Conversation conversation = new Conversation(id, ConversationType.GROUP, CURRENT_USER_ID, now, now);
        conversations.put(id, conversation);
        groups.put(id, new GroupDetails(id, CURRENT_USER_ID, request.name(), request.avatarUrl(),
                request.description(), now));
        members.add(new ConversationMember(id, CURRENT_USER_ID, MemberRole.OWNER, now, null, now));
        for (long userId : request.initialMemberIds()) {
            if (userId != CURRENT_USER_ID && users.containsKey(userId)) {
                members.add(new ConversationMember(id, userId, MemberRole.MEMBER, now, null, null));
            }
        }
        return summary(conversation);
    }

    public void removeGroupMember(long conversationId, long userId) {
        ConversationMember member = activeMember(conversationId, userId);
        if (member == null || member.role() == MemberRole.OWNER) return;
        members.remove(member);
        members.add(new ConversationMember(conversationId, userId, member.role(),
                member.joinedAt(), Instant.now(), member.lastReadAt()));
    }

    public void changeGroupRole(long conversationId, long userId, MemberRole role) {
        ConversationMember member = activeMember(conversationId, userId);
        if (member == null || member.role() == MemberRole.OWNER || role == MemberRole.OWNER) return;
        members.remove(member);
        members.add(new ConversationMember(conversationId, userId, role,
                member.joinedAt(), null, member.lastReadAt()));
    }

    private ConversationSummary summary(Conversation conversation) {
        String title;
        String avatarUrl = null;
        if (conversation.type() == ConversationType.GROUP) {
            GroupDetails details = groups.get(conversation.id());
            title = details.name();
            avatarUrl = details.avatarUrl();
        } else {
            User other = members.stream()
                    .filter(item -> item.conversationId() == conversation.id()
                            && item.userId() != CURRENT_USER_ID && item.leftAt() == null)
                    .map(item -> users.get(item.userId())).findFirst().orElse(null);
            title = other == null ? "Hội thoại" : other.displayName();
            if (other != null) avatarUrl = other.avatarUrl();
        }
        ChatMessage last = messages.getOrDefault(conversation.id(), List.of()).stream()
                .max(Comparator.comparing(ChatMessage::createdAt)).orElse(null);
        ConversationMember mine = activeMember(conversation.id(), CURRENT_USER_ID);
        int unread = (int) messages.getOrDefault(conversation.id(), List.of()).stream()
                .filter(message -> message.senderId() != null
                        && message.senderId() != CURRENT_USER_ID
                        && (mine.lastReadAt() == null || message.createdAt().isAfter(mine.lastReadAt())))
                .count();
        return new ConversationSummary(conversation, title, avatarUrl, last, unread);
    }

    private ConversationMember activeMember(long conversationId, long userId) {
        return members.stream().filter(item -> item.conversationId() == conversationId
                && item.userId() == userId && item.leftAt() == null).findFirst().orElse(null);
    }

    private void createDirectConversation(long otherUserId) {
        if (directConversationWith(otherUserId) != null) return;
        long id = nextConversationId++;
        Instant now = Instant.now();
        conversations.put(id, new Conversation(id, ConversationType.DIRECT, CURRENT_USER_ID, now, now));
        members.add(new ConversationMember(id, CURRENT_USER_ID, MemberRole.MEMBER, now, null, now));
        members.add(new ConversationMember(id, otherUserId, MemberRole.MEMBER, now, null, null));
    }

    private boolean isMine(Friendship friendship) {
        return friendship.requesterId() == CURRENT_USER_ID || friendship.addresseeId() == CURRENT_USER_ID;
    }

    private long otherUserId(Friendship friendship) {
        return friendship.requesterId() == CURRENT_USER_ID
                ? friendship.addresseeId() : friendship.requesterId();
    }

    private void seedUsers() {
        user(1, "an.nguyen", "Nguyễn Hoài An", 1, "227 Nguyễn Văn Cừ, Quận 5, TP.HCM",
                "2004-03-12", Gender.FEMALE, UserRole.USER, UserStatus.ACTIVE,
                "2026-10-01 08:00:00", "2026-11-05 08:00:00");
        user(2, "bao.tran", "Trần Gia Bảo", 3, "12 Lê Lợi, Quận 1, TP.HCM",
                "2004-07-25", Gender.MALE, UserRole.USER, UserStatus.ACTIVE,
                "2026-10-02 09:15:00", "2026-11-05 08:05:00");
        user(3, "chi.le", "Lê Minh Chi", 5, "45 Trần Hưng Đạo, Ninh Kiều, Cần Thơ",
                "2003-11-02", Gender.FEMALE, UserRole.USER, UserStatus.ACTIVE,
                "2026-10-03 10:30:00", "2026-11-05 08:10:00");
        user(4, "dung.pham", "Phạm Anh Dũng", 7, "8 Hùng Vương, Hải Châu, Đà Nẵng",
                "2004-01-30", Gender.MALE, UserRole.USER, UserStatus.ACTIVE,
                "2026-10-04 13:00:00", "2026-11-04 21:00:00");
        user(5, "ha.vo", "Võ Thanh Hà", 9, "102 Cách Mạng Tháng 8, Quận 3, TP.HCM",
                "2005-05-18", Gender.FEMALE, UserRole.USER, UserStatus.BLOCKED,
                "2026-10-05 14:20:00", "2026-11-04 22:00:00");
        user(6, "khanh.do", "Đỗ Minh Khánh", 0, "33 Kim Mã, Ba Đình, Hà Nội",
                "2004-09-09", Gender.OTHER, UserRole.USER, UserStatus.ACTIVE,
                "2026-10-06 15:45:00", "2026-11-05 07:55:00");
        user(7, "admin.linh", "Quản trị viên Linh", 0, "227 Nguyễn Văn Cừ, Quận 5, TP.HCM",
                "1995-04-20", Gender.FEMALE, UserRole.ADMIN, UserStatus.ACTIVE,
                "2026-09-01 08:00:00", "2026-11-05 07:30:00");
        user(8, "admin.nam", "Quản trị viên Nam", 0, "227 Nguyễn Văn Cừ, Quận 5, TP.HCM",
                "1993-12-01", Gender.MALE, UserRole.ADMIN, UserStatus.ACTIVE,
                "2026-09-01 08:05:00", "2026-11-05 07:35:00");
    }

    private void user(long id, String username, String name, int avatarNumber, String address,
                      String birthDate, Gender gender, UserRole role, UserStatus status,
                      String created, String updated) {
        String avatar = avatarNumber == 0 ? null : "https://i.pravatar.cc/300?img=" + avatarNumber;
        users.put(id, new User(id, username, username + "@example.com", name, avatar,
                address, LocalDate.parse(birthDate), gender, role, status,
                at(created), at(updated)));
    }

    private void seedFriendships() {
        friendship(1, 1, 2, FriendshipStatus.ACCEPTED, "2026-10-10 09:00:00", "2026-10-10 09:15:00");
        friendship(2, 1, 3, FriendshipStatus.ACCEPTED, "2026-10-11 10:00:00", "2026-10-11 10:20:00");
        friendship(3, 2, 4, FriendshipStatus.ACCEPTED, "2026-10-12 11:00:00", "2026-10-12 11:30:00");
        friendship(4, 3, 4, FriendshipStatus.PENDING, "2026-11-04 19:00:00", null);
        friendship(5, 1, 5, FriendshipStatus.BLOCKED, "2026-10-15 08:00:00", "2026-10-16 08:00:00");
        friendship(6, 2, 6, FriendshipStatus.PENDING, "2026-11-05 07:00:00", null);
    }

    private void friendship(long id, long requester, long addressee, FriendshipStatus status,
                            String created, String responded) {
        friendships.add(new Friendship(id, requester, addressee, status, at(created), at(responded)));
    }

    private void seedConversations() {
        conversation(1, ConversationType.DIRECT, 1, "2026-10-10 09:16:00", "2026-11-05 08:12:00");
        conversation(2, ConversationType.DIRECT, 1, "2026-10-11 10:21:00", "2026-11-04 20:05:00");
        conversation(3, ConversationType.DIRECT, 2, "2026-10-12 11:31:00", "2026-11-03 18:00:00");
        conversation(4, ConversationType.GROUP, 1, "2026-10-20 08:00:00", "2026-11-05 08:30:00");
        conversation(5, ConversationType.GROUP, 2, "2026-10-22 19:00:00", "2026-11-04 22:15:00");
    }

    private void conversation(long id, ConversationType type, long creator, String created, String updated) {
        conversations.put(id, new Conversation(id, type, creator, at(created), at(updated)));
    }

    private void seedMembers() {
        member(1, 1, MemberRole.MEMBER, "2026-10-10 09:16:00", null, "2026-11-05 08:12:30");
        member(1, 2, MemberRole.MEMBER, "2026-10-10 09:16:00", null, "2026-11-05 08:13:00");
        member(2, 1, MemberRole.MEMBER, "2026-10-11 10:21:00", null, "2026-11-04 20:06:00");
        member(2, 3, MemberRole.MEMBER, "2026-10-11 10:21:00", null, "2026-11-04 20:06:00");
        member(3, 2, MemberRole.MEMBER, "2026-10-12 11:31:00", null, "2026-11-03 18:01:00");
        member(3, 4, MemberRole.MEMBER, "2026-10-12 11:31:00", null, "2026-11-03 18:01:00");
        member(4, 1, MemberRole.OWNER, "2026-10-20 08:00:00", null, "2026-11-05 08:31:00");
        member(4, 2, MemberRole.ADMIN, "2026-10-20 08:05:00", null, "2026-11-05 08:31:00");
        member(4, 3, MemberRole.MEMBER, "2026-10-20 08:10:00", null, "2026-11-05 08:29:00");
        member(4, 4, MemberRole.MEMBER, "2026-10-21 09:00:00", null, "2026-11-04 22:00:00");
        member(4, 5, MemberRole.MEMBER, "2026-10-22 09:00:00", "2026-11-04 22:10:00",
                "2026-11-04 22:05:00");
        member(4, 6, MemberRole.MEMBER, "2026-10-23 14:00:00", null, null);
        member(5, 2, MemberRole.OWNER, "2026-10-22 19:00:00", null, "2026-11-04 22:16:00");
        member(5, 3, MemberRole.ADMIN, "2026-10-22 19:05:00", null, "2026-11-04 22:16:00");
        member(5, 5, MemberRole.MEMBER, "2026-10-22 19:10:00", "2026-11-04 22:20:00",
                "2026-11-04 22:15:30");
    }

    private void member(long conversationId, long userId, MemberRole role,
                        String joined, String left, String lastRead) {
        members.add(new ConversationMember(conversationId, userId, role,
                at(joined), at(left), at(lastRead)));
    }

    private void seedGroups() {
        groups.put(4L, new GroupDetails(4, 1, "Java Study Group",
                "https://example.com/groups/java-study.png",
                "Cùng học Java, Maven, JDBC và JavaFX.", at("2026-10-20 08:00:00")));
        groups.put(5L, new GroupDetails(5, 2, "Chuyện cuối ngày", null,
                "Nhóm trò chuyện và chia sẻ sau giờ học.", at("2026-10-22 19:00:00")));
    }

    private void seedMessages() {
        message(1, 1, 1, null, "Chào Bảo, hôm nay bạn học Java đến đâu rồi?",
                ChatMessageType.TEXT, ChatMessageStatus.SENT, "2026-11-05 08:10:00", null);
        message(2, 1, 2, 1L, "Mình đang làm phần JDBC, lát nữa gửi bạn xem nhé.",
                ChatMessageType.TEXT, ChatMessageStatus.SENT, "2026-11-05 08:12:00", null);
        message(3, 2, 3, null, "An ơi, tối nay họp nhóm lúc 20 giờ nhé.",
                ChatMessageType.TEXT, ChatMessageStatus.SENT, "2026-11-04 20:00:00", null);
        message(4, 2, 1, 3L, "Ok Chi, mình sẽ tham gia đúng giờ.",
                ChatMessageType.TEXT, ChatMessageStatus.EDITED, "2026-11-04 20:03:00",
                "2026-11-04 20:05:00");
        message(5, 3, 2, null, "Dũng kiểm tra giúp mình phần giao diện đăng nhập nhé.",
                ChatMessageType.TEXT, ChatMessageStatus.SENT, "2026-11-03 17:58:00", null);
        message(6, 4, 1, null, "Mình gửi sơ đồ ERD phiên bản đầu tiên.",
                ChatMessageType.TEXT, ChatMessageStatus.SENT, "2026-11-05 08:20:00", null);
        message(7, 4, 1, 6L, null,
                ChatMessageType.IMAGE, ChatMessageStatus.SENT, "2026-11-05 08:20:10", null);
        message(8, 4, 2, 6L, "Quan hệ conversation và member nhìn ổn rồi.",
                ChatMessageType.TEXT, ChatMessageStatus.SENT, "2026-11-05 08:24:00", null);
        message(9, 4, 5, null, "Bấm vào link này để nhận quà miễn phí!!!",
                ChatMessageType.TEXT, ChatMessageStatus.SENT, "2026-11-04 21:55:00", null);
        message(10, 4, 7, 9L, "Tin nhắn đã được quản trị viên ghi nhận để kiểm tra.",
                ChatMessageType.SYSTEM, ChatMessageStatus.SENT, "2026-11-04 22:05:00", null);
        message(11, 5, 2, null, "Mọi người hoàn thành task hôm nay chưa?",
                ChatMessageType.TEXT, ChatMessageStatus.SENT, "2026-11-04 22:10:00", null);
        message(12, 5, 3, 11L, "Mình hoàn thành phần giao diện chat rồi.",
                ChatMessageType.TEXT, ChatMessageStatus.SENT, "2026-11-04 22:15:00", null);
        messages.values().forEach(items ->
                items.sort(Comparator.comparing(ChatMessage::createdAt)));
    }

    private void message(long id, long conversationId, long senderId, Long replyToId,
                         String content, ChatMessageType type, ChatMessageStatus status,
                         String created, String edited) {
        List<MessageAttachment> attachments = switch ((int) id) {
            case 7 -> List.of(new MessageAttachment(1, 7, "java-chat-erd.png",
                    "https://example.com/uploads/java-chat-erd.png", "image/png", 248321));
            case 8 -> List.of(new MessageAttachment(2, 8, "review-notes.pdf",
                    "https://example.com/uploads/review-notes.pdf", "application/pdf", 512044));
            default -> List.of();
        };
        messages.computeIfAbsent(conversationId, ignored -> new ArrayList<>())
                .add(new ChatMessage(id, conversationId, senderId, replyToId, content,
                        type, status, at(created), at(edited), attachments));
    }

    private static Instant at(String localDateTime) {
        return localDateTime == null ? null
                : LocalDateTime.parse(localDateTime.replace(' ', 'T')).atZone(SEED_ZONE).toInstant();
    }
}
