package com.javachat.user.ui.chat;

import com.javachat.shared.dto.ChatDtos.ConversationSummary;
import com.javachat.shared.model.ConversationType;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Searchable conversation navigation backed by the shared protocol DTO. */
public final class ConversationListPanel extends JPanel {
    private static final Color INK = new Color(30, 48, 72);
    private static final Color MUTED = new Color(113, 132, 157);
    private static final Color BLUE = new Color(45, 112, 220);
    private static final Color PALE_BLUE = new Color(231, 241, 253);
    private static final Color BORDER = new Color(226, 235, 245);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM");

    private final List<ConversationSummary> all = new ArrayList<>();
    private final DefaultListModel<ConversationSummary> model = new DefaultListModel<>();
    private final JList<ConversationSummary> list = new JList<>(model);
    private final JScrollPane scroll = new JScrollPane(list);
    private final JTextField search = new JTextField() {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!getText().isEmpty() || hasFocus()) return;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(MUTED);
            g.setFont(getFont());
            var metrics = g.getFontMetrics();
            g.drawString("Tìm hội thoại", 0, (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
        }
    };
    private final PillButton allButton = new PillButton("Tất cả");
    private final PillButton unreadButton = new PillButton("Chưa đọc");
    private final JLabel empty = new JLabel("Chưa có hội thoại", SwingConstants.CENTER);
    private final JPanel center = new JPanel(new BorderLayout());
    private boolean unreadOnly;
    private Consumer<ConversationSummary> selectionListener = ignored -> { };

    public ConversationListPanel() {
        super(new BorderLayout());
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(350, 600));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));

        JPanel top = new JPanel();
        top.setBackground(Color.WHITE);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(BorderFactory.createEmptyBorder(24, 22, 14, 22));

        JLabel brand = new JLabel("●  JAVA CHAT");
        brand.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        brand.setForeground(BLUE);
        brand.setAlignmentX(LEFT_ALIGNMENT);
        top.add(brand);
        top.add(Box.createVerticalStrut(21));

        JLabel heading = new JLabel("Tin nhắn");
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        heading.setForeground(INK);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        top.add(heading);
        top.add(Box.createVerticalStrut(18));

        JPanel searchBox = new RoundedPanel(new Color(245, 248, 252), 14);
        searchBox.setLayout(new BorderLayout(10, 0));
        searchBox.setBorder(BorderFactory.createEmptyBorder(0, 13, 0, 13));
        searchBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        searchBox.setPreferredSize(new Dimension(300, 42));
        searchBox.setAlignmentX(LEFT_ALIGNMENT);
        searchBox.add(new SearchGlyph(), BorderLayout.WEST);
        search.setBorder(null);
        search.setOpaque(false);
        search.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        search.setForeground(INK);
        search.putClientProperty("JTextField.placeholderText", "Tìm hội thoại");
        search.setToolTipText("Tìm theo tên hoặc nội dung tin nhắn");
        searchBox.add(search, BorderLayout.CENTER);
        top.add(searchBox);
        top.add(Box.createVerticalStrut(16));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.setAlignmentX(LEFT_ALIGNMENT);
        filters.add(allButton);
        filters.add(unreadButton);
        top.add(filters);
        top.add(Box.createVerticalStrut(6));
        add(top, BorderLayout.NORTH);

        list.setBackground(Color.WHITE);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new ConversationRenderer());
        list.setFixedCellHeight(82);
        list.setBorder(BorderFactory.createEmptyBorder(2, 10, 8, 10));
        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) selectionListener.accept(list.getSelectedValue());
        });
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        center.setBackground(Color.WHITE);
        center.add(scroll, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        empty.setForeground(MUTED);
        empty.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { refresh(); }
            public void removeUpdate(DocumentEvent event) { refresh(); }
            public void changedUpdate(DocumentEvent event) { refresh(); }
        });
        allButton.addActionListener(event -> { unreadOnly = false; refresh(); });
        unreadButton.addActionListener(event -> { unreadOnly = true; refresh(); });
        refresh();
    }

    public void setSelectionListener(Consumer<ConversationSummary> listener) {
        selectionListener = listener == null ? ignored -> { } : listener;
    }

    public void selectConversation(long conversationId) {
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i).conversation().id() == conversationId) {
                if (list.getSelectedIndex() == i) selectionListener.accept(model.get(i));
                else list.setSelectedIndex(i);
                list.ensureIndexIsVisible(i);
                return;
            }
        }
    }

    /** Call on the Swing event dispatch thread after receiving a list response. */
    public void setConversations(List<ConversationSummary> conversations) {
        long selectedId = list.getSelectedValue() == null ? -1 : list.getSelectedValue().conversation().id();
        all.clear();
        if (conversations != null) {
            all.addAll(conversations.stream().filter(item -> item != null && item.conversation() != null).toList());
        }
        all.sort(Comparator.comparing(ConversationListPanel::activity, Comparator.nullsLast(Comparator.reverseOrder())));
        refresh();
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i).conversation().id() == selectedId) {
                list.setSelectedIndex(i);
                break;
            }
        }
    }

    private void refresh() {
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        model.clear();
        for (ConversationSummary item : all) {
            if (unreadOnly && item.unreadCount() <= 0) continue;
            if (title(item).toLowerCase(Locale.ROOT).contains(query)
                    || preview(item).toLowerCase(Locale.ROOT).contains(query)) model.addElement(item);
        }
        allButton.setActive(!unreadOnly);
        unreadButton.setActive(unreadOnly);
        center.removeAll();
        if (model.isEmpty()) {
            empty.setText(all.isEmpty() ? "Chưa có hội thoại"
                    : unreadOnly && query.isEmpty() ? "Không có hội thoại chưa đọc"
                    : "Không tìm thấy hội thoại");
            center.add(empty, BorderLayout.CENTER);
        } else {
            center.add(scroll, BorderLayout.CENTER);
        }
        center.revalidate();
        center.repaint();
    }

    static String title(ConversationSummary item) {
        return item.title() == null || item.title().isBlank() ? "Hội thoại " + item.conversation().id() : item.title();
    }

    static String preview(ConversationSummary item) {
        if (item.lastMessage() == null) return "Chưa có tin nhắn";
        String content = item.lastMessage().content();
        if (content != null && !content.isBlank()) return content.replace('\n', ' ');
        return "Đã gửi tệp đính kèm";
    }

    private static Instant activity(ConversationSummary item) {
        if (item.lastMessage() != null && item.lastMessage().createdAt() != null) return item.lastMessage().createdAt();
        return item.conversation().updatedAt();
    }

    private static String time(ConversationSummary item) {
        Instant instant = activity(item);
        if (instant == null) return "";
        var local = instant.atZone(ZoneId.systemDefault());
        return local.toLocalDate().equals(LocalDate.now()) ? TIME.format(local) : DATE.format(local);
    }

    /** Compact initials avatar shared with the chat header. */
    static final class Avatar extends JPanel {
        private final String initials;
        private final boolean group;

        Avatar(String title, boolean group, int size) {
            this.group = group;
            String[] words = title.strip().split("\\s+");
            initials = group ? "#" : words.length > 1
                    ? (words[0].substring(0, 1) + words[words.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT)
                    : title.substring(0, 1).toUpperCase(Locale.ROOT);
            setOpaque(false);
            setPreferredSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(group ? new Color(232, 225, 251) : new Color(219, 234, 252));
            int size = Math.min(getWidth(), getHeight());
            g.fillOval(0, 0, size, size);
            g.setColor(group ? new Color(111, 78, 181) : BLUE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, group ? 21 : 15));
            var metrics = g.getFontMetrics();
            g.drawString(initials, (size - metrics.stringWidth(initials)) / 2,
                    (size - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
        }
    }

    private static final class ConversationRenderer extends JPanel implements ListCellRenderer<ConversationSummary> {
        private final JPanel avatarSlot = new JPanel(new BorderLayout());
        private final JLabel name = new JLabel();
        private final JLabel snippet = new JLabel();
        private final JLabel timestamp = new JLabel();
        private final JLabel badge = new JLabel("", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(BLUE);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        private boolean selected;

        ConversationRenderer() {
            super(new BorderLayout(12, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(11, 12, 11, 12));
            avatarSlot.setOpaque(false);
            avatarSlot.setPreferredSize(new Dimension(46, 46));
            add(avatarSlot, BorderLayout.WEST);
            JPanel texts = new JPanel(new BorderLayout(0, 5));
            texts.setOpaque(false);
            name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            name.setForeground(INK);
            snippet.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            snippet.setForeground(MUTED);
            texts.add(name, BorderLayout.NORTH);
            texts.add(snippet, BorderLayout.CENTER);
            add(texts, BorderLayout.CENTER);
            JPanel meta = new JPanel();
            meta.setOpaque(false);
            meta.setLayout(new BoxLayout(meta, BoxLayout.Y_AXIS));
            timestamp.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            timestamp.setForeground(MUTED);
            timestamp.setAlignmentX(RIGHT_ALIGNMENT);
            badge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            badge.setForeground(Color.WHITE);
            badge.setOpaque(false);
            badge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            badge.setAlignmentX(RIGHT_ALIGNMENT);
            meta.add(timestamp);
            meta.add(Box.createVerticalStrut(7));
            meta.add(badge);
            add(meta, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ConversationSummary> source,
                ConversationSummary item, int index, boolean selected, boolean focused) {
            this.selected = selected;
            String title = title(item);
            name.setText(clip(title, 23));
            snippet.setText(clip(preview(item), 29));
            timestamp.setText(time(item));
            avatarSlot.removeAll();
            avatarSlot.add(new Avatar(title, item.conversation().type() == ConversationType.GROUP, 46),
                    BorderLayout.CENTER);
            int unread = Math.max(0, item.unreadCount());
            badge.setText(unread > 99 ? "99+" : Integer.toString(unread));
            badge.setVisible(unread > 0);
            repaint();
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(selected ? PALE_BLUE : Color.WHITE);
            g.fillRoundRect(0, 2, getWidth(), getHeight() - 4, 18, 18);
            g.dispose();
            super.paintComponent(graphics);
        }

        private static String clip(String value, int limit) {
            return value.length() > limit ? value.substring(0, limit - 1) + "…" : value;
        }
    }

    private static final class RoundedPanel extends JPanel {
        private final Color fill;
        private final int radius;

        RoundedPanel(Color fill, int radius) {
            this.fill = fill;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class SearchGlyph extends JPanel {
        SearchGlyph() {
            setOpaque(false);
            setPreferredSize(new Dimension(19, 19));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(MUTED);
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawOval(2, 2, 10, 10);
            g.drawLine(11, 11, 17, 17);
            g.dispose();
        }
    }

    private static final class PillButton extends JButton {
        private boolean active;

        PillButton(String label) {
            super(label);
            setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            setBorder(BorderFactory.createEmptyBorder(7, 13, 7, 13));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }

        void setActive(boolean active) {
            this.active = active;
            setForeground(active ? BLUE : MUTED);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(active ? PALE_BLUE : new Color(246, 248, 251));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
