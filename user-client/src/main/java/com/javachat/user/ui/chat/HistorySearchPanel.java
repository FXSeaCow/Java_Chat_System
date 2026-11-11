package com.javachat.user.ui.chat;

import com.javachat.shared.model.ChatMessage;
import com.javachat.shared.model.ChatMessageStatus;
import com.javachat.shared.model.ChatMessageType;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.text.Normalizer;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.LongConsumer;

/** Searches messages already loaded for the active conversation. */
final class HistorySearchPanel extends JPanel {
    private static final Color INK = new Color(30, 48, 72);
    private static final Color MUTED = new Color(113, 132, 157);
    private static final Color BLUE = new Color(45, 112, 220);
    private static final Color BORDER = new Color(220, 232, 244);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm");

    private final JTextField query = new JTextField();
    private final JLabel scope = new JLabel();
    private final JLabel count = new JLabel();
    private final JPanel results = new ResultStack();
    private final List<ChatMessage> messages = new ArrayList<>();
    private LongConsumer selectMessage = ignored -> { };
    private Runnable close = () -> { };
    private long currentUserId;

    HistorySearchPanel() {
        super(new BorderLayout());
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(310, 600));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER));

        JPanel top = new JPanel();
        top.setBackground(Color.WHITE);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(BorderFactory.createEmptyBorder(20, 18, 14, 18));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        JLabel title = new JLabel("Tìm trong hội thoại");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        title.setForeground(INK);
        heading.add(title, BorderLayout.CENTER);
        JButton closeButton = new JButton("×");
        closeButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
        closeButton.setForeground(MUTED);
        closeButton.setBorder(null);
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.setToolTipText("Đóng tìm kiếm");
        closeButton.addActionListener(event -> close.run());
        heading.add(closeButton, BorderLayout.EAST);
        top.add(heading);
        top.add(Box.createVerticalStrut(7));
        scope.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        scope.setForeground(MUTED);
        scope.setAlignmentX(LEFT_ALIGNMENT);
        top.add(scope);
        top.add(Box.createVerticalStrut(15));
        query.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        query.setForeground(INK);
        query.setToolTipText("Tìm theo nội dung tin nhắn đã tải");
        query.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(10, 11, 10, 11)));
        query.setMaximumSize(new Dimension(Integer.MAX_VALUE, 39));
        query.setAlignmentX(LEFT_ALIGNMENT);
        top.add(query);
        top.add(Box.createVerticalStrut(11));
        count.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        count.setForeground(MUTED);
        count.setAlignmentX(LEFT_ALIGNMENT);
        top.add(count);
        add(top, BorderLayout.NORTH);

        results.setLayout(new BoxLayout(results, BoxLayout.Y_AXIS));
        results.setBackground(Color.WHITE);
        results.setBorder(BorderFactory.createEmptyBorder(0, 10, 14, 10));
        JScrollPane scroll = new JScrollPane(results);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        query.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { refresh(); }
            public void removeUpdate(DocumentEvent event) { refresh(); }
            public void changedUpdate(DocumentEvent event) { refresh(); }
        });
        refresh();
    }

    void setConversationTitle(String title) {
        scope.setText("Trong: " + title);
        query.setText("");
        refresh();
    }

    void setCurrentUserId(long userId) {
        currentUserId = userId;
        refresh();
    }

    void setMessages(List<ChatMessage> history) {
        messages.clear();
        if (history != null) messages.addAll(history);
        refresh();
    }

    void setSelectMessageListener(LongConsumer listener) {
        selectMessage = listener == null ? ignored -> { } : listener;
    }

    void setCloseListener(Runnable listener) {
        close = listener == null ? () -> { } : listener;
    }

    void focusSearch() {
        query.requestFocusInWindow();
    }

    private void refresh() {
        results.removeAll();
        String needle = fold(query.getText().strip());
        if (needle.isEmpty()) {
            count.setText("Tìm trong các tin nhắn đã tải");
            showHint("Nhập từ khóa để bắt đầu tìm kiếm");
        } else {
            int matches = 0;
            for (ChatMessage message : messages) {
                if (message == null || message.status() == ChatMessageStatus.DELETED
                        || message.messageType() == ChatMessageType.SYSTEM) continue;
                String content = message.content();
                if (content == null || !fold(content).contains(needle)) continue;
                results.add(resultButton(message));
                results.add(Box.createVerticalStrut(5));
                matches++;
            }
            count.setText(matches + " kết quả");
            if (matches == 0) showHint("Không tìm thấy tin nhắn phù hợp");
        }
        results.revalidate();
        results.repaint();
    }

    private void showHint(String text) {
        JLabel hint = new JLabel(text, SwingConstants.CENTER);
        hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        hint.setForeground(MUTED);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        hint.setBorder(BorderFactory.createEmptyBorder(26, 7, 0, 7));
        results.add(hint);
    }

    private JButton resultButton(ChatMessage message) {
        JButton button = new JButton();
        button.setLayout(new BorderLayout(0, 6));
        button.setBackground(new Color(247, 250, 254));
        button.setOpaque(true);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(10, 11, 10, 11)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        JLabel sender = new JLabel(message.senderId() != null && message.senderId() == currentUserId
                ? "Bạn" : "Tin nhắn nhận được");
        sender.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        sender.setForeground(BLUE);
        JLabel snippet = new JLabel(clip(message.content().replaceAll("\\s+", " "), 56));
        snippet.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        snippet.setForeground(INK);
        JLabel when = new JLabel(message.createdAt() == null ? "" : WHEN.format(
                message.createdAt().atZone(ZoneId.systemDefault())));
        when.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        when.setForeground(MUTED);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(sender, BorderLayout.WEST);
        top.add(when, BorderLayout.EAST);
        button.add(top, BorderLayout.NORTH);
        button.add(snippet, BorderLayout.CENTER);
        button.addActionListener(event -> selectMessage.accept(message.id()));
        return button;
    }

    private static String clip(String text, int limit) {
        return text.length() > limit ? text.substring(0, limit - 1) + "…" : text;
    }

    private static String fold(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    private static final class ResultStack extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 18;
        }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(18, visibleRect.height - 30);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
