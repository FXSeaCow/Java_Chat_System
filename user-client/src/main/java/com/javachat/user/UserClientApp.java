package com.javachat.user;

import com.javachat.shared.dto.ChatDtos.ConversationSummary;
import com.javachat.shared.model.ChatMessage;
import com.javachat.shared.model.ChatMessageStatus;
import com.javachat.shared.model.ChatMessageType;
import com.javachat.shared.model.Conversation;
import com.javachat.shared.model.ConversationType;
import com.javachat.user.ui.chat.MainFrame;

import javax.swing.SwingUtilities;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
        frame.setConversations(demoConversations(now));
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
