package com.javachat.user.ui.chat;

import com.javachat.shared.dto.ChatDtos.ConversationSummary;
import com.javachat.shared.dto.ChatDtos.MessageSendRequest;
import com.javachat.shared.model.ChatMessage;
import com.javachat.shared.model.ChatMessageStatus;
import com.javachat.shared.model.ChatMessageType;
import com.javachat.shared.model.ConversationType;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Chat detail UI. Network and history loading are supplied by MainFrame's callbacks. */
public final class ChatPanel extends JPanel {
    private static final Color INK = new Color(30, 48, 72);
    private static final Color MUTED = new Color(113, 132, 157);
    private static final Color BLUE = new Color(45, 112, 220);
    private static final Color CANVAS = new Color(238, 245, 252);
    private static final Color BORDER = new Color(220, 232, 244);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JPanel messageList = new MessageStack();
    private final JScrollPane scroll;
    private final JPanel footer;
    private final JPanel south = new JPanel(new BorderLayout());
    private final JPanel body = new JPanel(new BorderLayout());
    private final JTextArea composer = new JTextArea(2, 20) {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!getText().isEmpty() || hasFocus()) return;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(MUTED);
            g.setFont(getFont());
            g.drawString("Nhập tin nhắn...", 0, getFontMetrics(getFont()).getAscent());
            g.dispose();
        }
    };
    private final JButton sendButton = new SendButton();
    private final JLabel empty = new JLabel("Chưa có tin nhắn. Hãy bắt đầu cuộc trò chuyện!", SwingConstants.CENTER);
    private final List<ChatMessage> messages = new ArrayList<>();
    private ConversationSummary conversation;
    private long currentUserId;
    private Consumer<MessageSendRequest> sendListener;

    public ChatPanel() {
        super(new BorderLayout());
        setBackground(CANVAS);
        messageList.setLayout(new BoxLayout(messageList, BoxLayout.Y_AXIS));
        messageList.setBackground(CANVAS);
        messageList.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        scroll = new JScrollPane(messageList);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(CANVAS);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        body.setBackground(CANVAS);
        body.add(scroll, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
        footer = createComposer();
        south.add(footer, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);
        empty.setForeground(MUTED);
        empty.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        renderMessages();
    }

    public void setCurrentUserId(long userId) {
        currentUserId = userId;
        renderMessages();
    }

    public void setSendListener(Consumer<MessageSendRequest> listener) {
        sendListener = listener;
        updateSendState();
    }

    public long conversationId() {
        return conversation == null ? -1 : conversation.conversation().id();
    }

    public void setConversation(ConversationSummary item) {
        if (item == null || item.conversation() == null) return;
        boolean changed = conversationId() != item.conversation().id();
        conversation = item;
        if (changed) {
            messages.clear();
            composer.setText("");
        }
        removeAll();
        add(createHeader(item), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        renderMessages();
        updateSendState();
        revalidate();
        repaint();
    }

    /** Replace history for the visible conversation. Older pages can be included in the supplied list. */
    public void setMessages(List<ChatMessage> history) {
        messages.clear();
        if (history != null) {
            history.stream().filter(message -> message != null && message.conversationId() == conversationId())
                    .forEach(messages::add);
        }
        messages.sort(Comparator.comparing(ChatMessage::createdAt,
                Comparator.nullsLast(Comparator.naturalOrder())).thenComparingLong(ChatMessage::id));
        renderMessages();
    }

    /** Add a server event or an acknowledged local demo message without duplicating its ID. */
    public void appendMessage(ChatMessage message) {
        if (message == null || message.conversationId() != conversationId()) return;
        messages.removeIf(existing -> existing.id() == message.id());
        messages.add(message);
        messages.sort(Comparator.comparing(ChatMessage::createdAt,
                Comparator.nullsLast(Comparator.naturalOrder())).thenComparingLong(ChatMessage::id));
        renderMessages();
    }

    private JPanel createHeader(ConversationSummary item) {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(17, 26, 17, 26)));
        String title = ConversationListPanel.title(item);
        header.add(new ConversationListPanel.Avatar(title,
                item.conversation().type() == ConversationType.GROUP, 42), BorderLayout.WEST);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(title);
        name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        name.setForeground(INK);
        JLabel type = new JLabel(item.conversation().type() == ConversationType.GROUP
                ? "Hội thoại nhóm" : "Hội thoại riêng");
        type.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        type.setForeground(MUTED);
        labels.add(name);
        labels.add(Box.createVerticalStrut(3));
        labels.add(type);
        header.add(labels, BorderLayout.CENTER);
        return header;
    }

    private JPanel createComposer() {
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)));
        JPanel input = new Bubble(new Color(245, 248, 252), 20);
        input.setLayout(new BorderLayout());
        input.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        composer.setLineWrap(true);
        composer.setWrapStyleWord(true);
        composer.setOpaque(false);
        composer.setBorder(null);
        composer.setForeground(INK);
        composer.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        composer.setRows(2);
        composer.setToolTipText("Nhấn Enter để gửi, Shift+Enter để xuống dòng");
        composer.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "send-message");
        composer.getActionMap().put("send-message", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { sendMessage(); }
        });
        composer.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("shift ENTER"), "insert-break");
        composer.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { updateSendState(); }
            public void removeUpdate(DocumentEvent event) { updateSendState(); }
            public void changedUpdate(DocumentEvent event) { updateSendState(); }
        });
        input.add(composer, BorderLayout.CENTER);
        footer.add(input, BorderLayout.CENTER);
        sendButton.setPreferredSize(new Dimension(46, 46));
        sendButton.setToolTipText("Gửi tin nhắn");
        sendButton.addActionListener(event -> sendMessage());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(sendButton);
        footer.add(buttons, BorderLayout.EAST);
        updateSendState();
        return footer;
    }

    private void updateSendState() {
        sendButton.setEnabled(conversation != null && sendListener != null && !composer.getText().isBlank());
    }

    private void sendMessage() {
        if (conversation == null || sendListener == null) return;
        String content = composer.getText().strip();
        if (content.isEmpty()) return;
        MessageSendRequest request = new MessageSendRequest(conversationId(), UUID.randomUUID(), null,
                ChatMessageType.TEXT, content, List.of());
        sendListener.accept(request);
        composer.setText("");
        composer.requestFocusInWindow();
    }

    private void renderMessages() {
        messageList.removeAll();
        if (messages.isEmpty()) {
            JPanel emptyRow = new JPanel(new java.awt.GridBagLayout());
            emptyRow.setOpaque(false);
            emptyRow.setAlignmentX(LEFT_ALIGNMENT);
            emptyRow.setPreferredSize(new Dimension(0, 260));
            emptyRow.add(empty);
            messageList.add(emptyRow);
        } else {
            LocalDate lastDay = null;
            for (ChatMessage message : messages) {
                LocalDate day = message.createdAt() == null ? null
                        : message.createdAt().atZone(ZoneId.systemDefault()).toLocalDate();
                if (day != null && !day.equals(lastDay)) {
                    messageList.add(dayLabel(day));
                    lastDay = day;
                }
                JPanel row = messageRow(message);
                messageList.add(row);
                messageList.add(Box.createVerticalStrut(10));
            }
        }
        messageList.revalidate();
        messageList.repaint();
        javax.swing.SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar()
                .setValue(scroll.getVerticalScrollBar().getMaximum()));
    }

    private JPanel dayLabel(LocalDate day) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel label = new JLabel(day.equals(LocalDate.now()) ? "Hôm nay" : DAY.format(day));
        label.setForeground(MUTED);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        row.add(label);
        return row;
    }

    private JPanel messageRow(ChatMessage message) {
        boolean own = message.senderId() != null && message.senderId() == currentUserId;
        boolean system = message.messageType() == ChatMessageType.SYSTEM;
        JPanel row = new JPanel(new FlowLayout(system ? FlowLayout.CENTER
                : own ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        if (system) {
            JLabel label = new JLabel(content(message));
            label.setForeground(MUTED);
            label.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
            row.add(label);
            return row;
        }
        JPanel bubble = new Bubble(own ? BLUE : Color.WHITE, 18);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(BorderFactory.createEmptyBorder(10, 14, 8, 14));
        String body = content(message);
        JTextArea text = new JTextArea(body);
        text.setEditable(false);
        text.setFocusable(false);
        text.setOpaque(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        text.setForeground(own ? Color.WHITE : INK);
        int width = Math.min(340, Math.max(70, longestLineWidth(text, body) + 8));
        text.setSize(new Dimension(width, Short.MAX_VALUE));
        Dimension textSize = text.getPreferredSize();
        text.setPreferredSize(new Dimension(width, textSize.height));
        bubble.add(text);
        bubble.add(Box.createVerticalStrut(4));
        JLabel time = new JLabel(message.createdAt() == null ? "" : TIME.format(
                message.createdAt().atZone(ZoneId.systemDefault())));
        time.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        time.setForeground(own ? new Color(225, 238, 255) : MUTED);
        time.setAlignmentX(own ? RIGHT_ALIGNMENT : LEFT_ALIGNMENT);
        bubble.add(time);
        row.add(bubble);
        return row;
    }

    private static int longestLineWidth(JTextArea area, String body) {
        int longest = 0;
        for (String line : body.split("\\R", -1)) {
            longest = Math.max(longest, area.getFontMetrics(area.getFont()).stringWidth(line));
        }
        return longest;
    }

    private static String content(ChatMessage message) {
        if (message.status() == ChatMessageStatus.DELETED) return "Tin nhắn đã bị xóa";
        if (message.content() != null && !message.content().isBlank()) return message.content();
        return switch (message.messageType()) {
            case IMAGE -> "Đã gửi hình ảnh";
            case FILE -> "Đã gửi tệp đính kèm";
            default -> "";
        };
    }

    private static final class Bubble extends JPanel {
        private final Color color;
        private final int radius;

        Bubble(Color color, int radius) {
            this.color = color;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class MessageStack extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 20;
        }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(20, visibleRect.height - 40);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static final class SendButton extends JButton {
        SendButton() {
            setBorder(null);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(isEnabled() ? BLUE : new Color(177, 197, 221));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int mid = getHeight() / 2;
            g.drawLine(15, mid, 30, mid);
            g.drawLine(24, mid - 6, 30, mid);
            g.drawLine(24, mid + 6, 30, mid);
            g.dispose();
        }
    }
}
