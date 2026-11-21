package com.javachat.user.ui.chat;

import com.javachat.shared.dto.ChatDtos.ConversationSummary;
import com.javachat.shared.dto.ChatDtos.MessageSendRequest;
import com.javachat.shared.dto.AiDtos;
import com.javachat.shared.model.ChatMessage;
import com.javachat.shared.model.ChatMessageStatus;
import com.javachat.shared.model.ChatMessageType;
import com.javachat.shared.model.ConversationType;
import com.javachat.shared.model.MessageAttachment;

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
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Chat detail UI. Network and history loading are supplied by MainFrame's callbacks. */
public final class ChatPanel extends JPanel {
    private static final Color INK = ChatTheme.INK;
    private static final Color MUTED = ChatTheme.MUTED;
    private static final Color BLUE = ChatTheme.TEAL;
    private static final Color SKY = ChatTheme.TEAL;
    private static final Color CANVAS = ChatTheme.CREAM;
    private static final Color INPUT = ChatTheme.MILK;
    private static final Color BORDER = ChatTheme.BORDER;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JPanel messageList = new MessageStack();
    private final JScrollPane scroll;
    private final JPanel footer;
    private final JPanel south = new JPanel(new BorderLayout());
    private final LlmSuggestionPanel aiPanel = new LlmSuggestionPanel();
    private final JPanel body = new JPanel(new BorderLayout());
    private final HistorySearchPanel historySearch = new HistorySearchPanel();
    private final Map<Long, JPanel> messageRows = new HashMap<>();
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
    private long focusedMessageId = -1;
    private boolean searchOpen;
    private Consumer<MessageSendRequest> sendListener;
    private Consumer<AiDtos.ChatRequest> aiSuggestionListener;
    private boolean aiLoading;

    public ChatPanel() {
        super(new BorderLayout());
        setBackground(CANVAS);
        messageList.setLayout(new BoxLayout(messageList, BoxLayout.Y_AXIS));
        messageList.setBackground(CANVAS);
        messageList.setBorder(BorderFactory.createEmptyBorder(12, 33, 20, 33));
        scroll = new JScrollPane(messageList);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(CANVAS);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        body.setBackground(CANVAS);
        body.add(scroll, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
        footer = createComposer();
        south.add(aiPanel, BorderLayout.NORTH);
        south.add(footer, BorderLayout.SOUTH);
        south.setBackground(CANVAS);
        aiPanel.setVisible(false);
        aiPanel.setGenerateAction(this::requestAiSuggestion);
        aiPanel.setInsertAction(this::insertAiSuggestion);
        aiPanel.setCloseAction(() -> aiPanel.setVisible(false));
        add(south, BorderLayout.SOUTH);
        historySearch.setSelectMessageListener(this::focusMessage);
        historySearch.setCloseListener(() -> showSearch(false));
        empty.setForeground(MUTED);
        empty.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        renderMessages();
    }

    public void setCurrentUserId(long userId) {
        currentUserId = userId;
        historySearch.setCurrentUserId(userId);
        renderMessages();
    }

    public void setSendListener(Consumer<MessageSendRequest> listener) {
        sendListener = listener;
        updateSendState();
    }

    public void setAiSuggestionListener(Consumer<AiDtos.ChatRequest> listener) {
        aiSuggestionListener = listener;
        if (!aiLoading) aiPanel.reset(listener != null);
    }

    public void setAiSuggestionResult(long conversationId, AiDtos.ChatResponse response) {
        if (conversationId != conversationId() || !aiLoading) return;
        aiLoading = false;
        if (response == null || response.response() == null || response.response().isBlank()) {
            aiPanel.setError("AI không trả về gợi ý. Hãy thử lại.");
        } else {
            aiPanel.setSuggestion(response.response(), response.model());
        }
    }

    public void setAiSuggestionError(long conversationId, String message) {
        if (conversationId != conversationId() || !aiLoading) return;
        aiLoading = false;
        aiPanel.setError(message);
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
            focusedMessageId = -1;
            historySearch.setConversationTitle(ConversationListPanel.title(item));
            historySearch.setMessages(List.of());
            showSearch(false);
            aiLoading = false;
            aiPanel.reset(aiSuggestionListener != null);
            aiPanel.setVisible(false);
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
        historySearch.setMessages(messages);
        renderMessages();
    }

    /** Add a server event or an acknowledged local demo message without duplicating its ID. */
    public void appendMessage(ChatMessage message) {
        if (message == null || message.conversationId() != conversationId()) return;
        messages.removeIf(existing -> existing.id() == message.id());
        messages.add(message);
        messages.sort(Comparator.comparing(ChatMessage::createdAt,
                Comparator.nullsLast(Comparator.naturalOrder())).thenComparingLong(ChatMessage::id));
        historySearch.setMessages(messages);
        renderMessages();
    }

    private JPanel createHeader(ConversationSummary item) {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(ChatTheme.MILK);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(11, 20, 11, 20)));
        String title = ConversationListPanel.title(item);
        header.add(new ConversationListPanel.Avatar(title,
                item.conversation().type() == ConversationType.GROUP, 30), BorderLayout.WEST);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(title);
        name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        name.setForeground(INK);
        JLabel type = new JLabel(item.conversation().type() == ConversationType.GROUP
                ? "● Nhóm chat" : "● Đang hoạt động");
        type.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
        type.setForeground(ChatTheme.TEAL_DARK);
        labels.add(name);
        labels.add(Box.createVerticalStrut(3));
        labels.add(type);
        header.add(labels, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton find = new SearchButton();
        find.setPreferredSize(new Dimension(25, 25));
        find.setToolTipText("Tìm trong lịch sử chat");
        find.addActionListener(event -> showSearch(!searchOpen));
        actions.add(find);
        JButton call = headerIcon("☎", "Chức năng gọi đang phát triển");
        call.setEnabled(false);
        actions.add(call);
        JButton more = headerIcon("⋯", "Tùy chọn");
        JPopupMenu menu = new JPopupMenu();
        JMenuItem searchItem = new JMenuItem("Tìm trong hội thoại");
        searchItem.addActionListener(event -> showSearch(true));
        JMenuItem aiItem = new JMenuItem("Gợi ý AI");
        aiItem.addActionListener(event -> {
            aiPanel.setVisible(true);
            south.revalidate();
        });
        menu.add(searchItem);
        menu.add(aiItem);
        more.addActionListener(event -> menu.show(more, 0, more.getHeight()));
        actions.add(more);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private static JButton headerIcon(String symbol, String tooltip) {
        JButton button = new JButton(symbol);
        button.setPreferredSize(new Dimension(25, 25));
        button.setToolTipText(tooltip);
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        button.setForeground(INK);
        button.setBorder(null);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        return button;
    }

    private void showSearch(boolean visible) {
        searchOpen = visible;
        body.removeAll();
        body.add(visible ? historySearch : scroll, BorderLayout.CENTER);
        south.setVisible(!visible);
        body.revalidate();
        body.repaint();
        if (visible) javax.swing.SwingUtilities.invokeLater(historySearch::focusSearch);
    }

    private void focusMessage(long messageId) {
        if (messages.stream().noneMatch(message -> message.id() == messageId)) return;
        focusedMessageId = messageId;
        showSearch(false);
        renderMessages();
        javax.swing.SwingUtilities.invokeLater(() -> {
            JPanel row = messageRows.get(messageId);
            if (row != null) row.scrollRectToVisible(new Rectangle(0, 0, row.getWidth(), row.getHeight()));
        });
    }

    private JPanel createComposer() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(CANVAS);
        footer.setBorder(BorderFactory.createEmptyBorder(10, 15, 14, 15));
        JPanel input = new Bubble(INPUT, 14);
        input.setLayout(new BorderLayout());
        input.setBorder(BorderFactory.createEmptyBorder(7, 9, 7, 9));
        composer.setLineWrap(true);
        composer.setWrapStyleWord(true);
        composer.setOpaque(false);
        composer.setBorder(null);
        composer.setForeground(INK);
        composer.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        composer.setRows(1);
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
        JButton addButton = new JButton("+");
        addButton.setToolTipText("Gợi ý AI");
        addButton.setForeground(SKY);
        addButton.setBackground(ChatTheme.TEAL_PALE);
        addButton.setBorder(BorderFactory.createEmptyBorder());
        addButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        addButton.setPreferredSize(new Dimension(27, 27));
        addButton.addActionListener(event -> {
            aiPanel.setVisible(!aiPanel.isVisible());
            south.revalidate();
        });
        input.add(addButton, BorderLayout.WEST);
        composer.setBorder(BorderFactory.createEmptyBorder(7, 10, 0, 0));
        input.add(composer, BorderLayout.CENTER);
        sendButton.setPreferredSize(new Dimension(30, 30));
        sendButton.setToolTipText("Gửi tin nhắn");
        sendButton.addActionListener(event -> sendMessage());
        JPanel trailing = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        trailing.setOpaque(false);
        JButton emoji = headerIcon("😊", "Chèn emoji");
        JPopupMenu emojis = new JPopupMenu();
        for (String symbol : new String[] {"😊", "❤️", "👍", "😂", "🎉"}) {
            JMenuItem choice = new JMenuItem(symbol);
            choice.addActionListener(event -> composer.insert(symbol, composer.getCaretPosition()));
            emojis.add(choice);
        }
        emoji.addActionListener(event -> emojis.show(emoji, 0, -emojis.getPreferredSize().height));
        trailing.add(emoji);
        trailing.add(sendButton);
        input.add(trailing, BorderLayout.EAST);
        footer.add(input, BorderLayout.CENTER);
        updateSendState();
        return footer;
    }

    private void updateSendState() {
        sendButton.setEnabled(conversation != null && sendListener != null && !composer.getText().isBlank());
    }

    private void requestAiSuggestion() {
        if (conversation == null || aiLoading) return;
        if (aiSuggestionListener == null) {
            aiPanel.reset(false);
            return;
        }
        List<ChatMessage> context = messages.stream()
                .filter(message -> message.messageType() == ChatMessageType.TEXT
                        && message.status() != ChatMessageStatus.DELETED
                        && message.content() != null && !message.content().isBlank())
                .toList();
        if (context.isEmpty()) {
            aiPanel.setError("Chưa có tin nhắn để tạo gợi ý.");
            return;
        }
        StringBuilder prompt = new StringBuilder("Dựa trên hội thoại sau, gợi ý một câu trả lời ngắn gọn, tự nhiên bằng tiếng Việt. "
                + "Chỉ trả về nội dung tin nhắn, không giải thích.\n");
        for (ChatMessage message : context.subList(Math.max(0, context.size() - 6), context.size())) {
            prompt.append(message.senderId() != null && message.senderId() == currentUserId ? "Bạn: " : "Người kia: ")
                    .append(message.content().strip()).append('\n');
        }
        aiLoading = true;
        aiPanel.setLoading();
        try {
            aiSuggestionListener.accept(new AiDtos.ChatRequest(conversationId(), prompt.toString()));
        } catch (RuntimeException exception) {
            aiLoading = false;
            aiPanel.setError("Không thể gửi yêu cầu gợi ý. Hãy thử lại.");
        }
    }

    private void insertAiSuggestion() {
        String text = aiPanel.suggestion().strip();
        if (text.isEmpty()) return;
        String draft = composer.getText();
        composer.setText(draft.isBlank() ? text : draft.stripTrailing() + "\n" + text);
        composer.requestFocusInWindow();
        aiPanel.setVisible(false);
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
        messageRows.clear();
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
                messageRows.put(message.id(), row);
                messageList.add(row);
                messageList.add(Box.createVerticalStrut(15));
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
        JLabel label = new JLabel(day.equals(LocalDate.now()) ? "HÔM NAY" : DAY.format(day)) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(ChatTheme.TEAL_PALE);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        label.setForeground(MUTED);
        label.setOpaque(false);
        label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
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
        boolean hasFile = message.status() != ChatMessageStatus.DELETED
                && message.messageType() == ChatMessageType.FILE && !message.attachments().isEmpty();
        JPanel bubble = new Bubble(own && !hasFile ? BLUE : ChatTheme.MILK, 14,
                message.id() == focusedMessageId);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(BorderFactory.createEmptyBorder(9, 11, 9, 11));
        String body = content(message);
        if (hasFile) {
            bubble.add(attachmentCard(message.attachments().get(0)));
        } else {
            JTextArea text = new JTextArea(body);
            text.setEditable(false);
            text.setFocusable(false);
            text.setOpaque(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            text.setForeground(own ? Color.WHITE : INK);
            int width = Math.min(220, Math.max(70, longestLineWidth(text, body) + 8));
            text.setSize(new Dimension(width, Short.MAX_VALUE));
            Dimension textSize = text.getPreferredSize();
            text.setPreferredSize(new Dimension(width, textSize.height));
            bubble.add(text);
        }
        JLabel time = new JLabel(message.createdAt() == null ? "" : TIME.format(
                message.createdAt().atZone(ZoneId.systemDefault())));
        time.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
        time.setForeground(MUTED);
        time.setAlignmentX(own ? RIGHT_ALIGNMENT : LEFT_ALIGNMENT);
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        bubble.setAlignmentX(own ? RIGHT_ALIGNMENT : LEFT_ALIGNMENT);
        stack.add(bubble);
        stack.add(Box.createVerticalStrut(4));
        stack.add(time);
        row.add(stack);
        return row;
    }

    private static JPanel attachmentCard(MessageAttachment attachment) {
        JPanel card = new JPanel(new BorderLayout(9, 0));
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(200, 43));
        JLabel icon = new JLabel("▤", SwingConstants.CENTER);
        icon.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        icon.setForeground(BLUE);
        icon.setOpaque(true);
        icon.setBackground(ChatTheme.TEAL_PALE);
        icon.setPreferredSize(new Dimension(37, 37));
        card.add(icon, BorderLayout.WEST);
        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        String fileName = attachment.fileName() == null || attachment.fileName().isBlank()
                ? "Tệp đính kèm" : attachment.fileName();
        JLabel name = new JLabel(fileName.length() > 27 ? fileName.substring(0, 26) + "…" : fileName);
        name.setToolTipText(fileName);
        name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        name.setForeground(INK);
        String fileSize = attachment.fileSize() >= 1024 * 1024
                ? String.format(java.util.Locale.ROOT, "%.1f MB", attachment.fileSize() / 1048576.0)
                : Math.max(1, attachment.fileSize() / 1024) + " KB";
        JLabel size = new JLabel("Tệp đính kèm · " + fileSize);
        size.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
        size.setForeground(MUTED);
        details.add(Box.createVerticalStrut(4));
        details.add(name);
        details.add(Box.createVerticalStrut(3));
        details.add(size);
        card.add(details, BorderLayout.CENTER);
        return card;
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
        private final boolean highlighted;

        Bubble(Color color, int radius) {
            this(color, radius, false);
        }

        Bubble(Color color, int radius, boolean highlighted) {
            this.color = color;
            this.radius = radius;
            this.highlighted = highlighted;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            if (ChatTheme.MILK.equals(color)) {
                g.setColor(BORDER);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            if (highlighted) {
                g.setColor(new Color(242, 174, 39));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, radius, radius);
            }
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
            g.setColor(isEnabled() ? SKY : new Color(159, 216, 203));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int mid = getHeight() / 2;
            int tip = getWidth() / 2 + 6;
            g.drawLine(getWidth() / 2 - 6, mid, tip, mid);
            g.drawLine(tip - 5, mid - 5, tip, mid);
            g.drawLine(tip - 5, mid + 5, tip, mid);
            g.dispose();
        }
    }

    private static final class SearchButton extends JButton {
        SearchButton() {
            setBorder(null);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(INK);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawOval(7, 7, 9, 9);
            g.drawLine(15, 15, 20, 20);
            g.dispose();
        }
    }
}
