package com.javachat.user;

import com.javachat.shared.dto.ChatDtos.ConversationSummary;
import com.javachat.shared.dto.AiDtos;
import com.javachat.shared.model.ChatMessage;
import com.javachat.shared.model.ChatMessageStatus;
import com.javachat.shared.model.ChatMessageType;
import com.javachat.shared.model.Conversation;
import com.javachat.shared.model.ConversationType;
import com.javachat.user.ui.chat.MainFrame;
import com.javachat.user.ui.auth.LoginFrame;

import javax.swing.SwingUtilities;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class UserClientApp {
    private UserClientApp() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            boolean demo = List.of(args).contains("--demo");
            LoginFrame login = new LoginFrame(() -> {
                MainFrame frame = new MainFrame();
                if (demo) setupDemo(frame);
                frame.setVisible(true);
            });
            login.setVisible(true);
        });
    }

    private static void setupDemo(MainFrame frame) {
        Instant now = Instant.now();
        Map<Long, List<ChatMessage>> history = demoHistory(now);
        AtomicLong nextId = new AtomicLong(1000);
        frame.setCurrentUserId(1L);
        frame.setConversations(demoConversations(now));
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
}
