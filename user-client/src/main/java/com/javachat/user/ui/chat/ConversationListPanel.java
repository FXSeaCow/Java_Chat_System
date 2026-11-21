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
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
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
    private static final Color INK = ChatTheme.INK;
    private static final Color MUTED = ChatTheme.MUTED;
    private static final Color BLUE = ChatTheme.TEAL;
    private static final Color PALE_BLUE = ChatTheme.TEAL_PALE;
    private static final Color BORDER = ChatTheme.BORDER;
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
            g.drawString("Search conversations", getInsets().left,
                    (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
        }
    };
    private final PillButton allButton = new PillButton("Tất cả");
    private final PillButton unreadButton = new PillButton("Chưa đọc");
    private final JButton actionsButton = new RoundedActionButton();
    private final JLabel empty = new JLabel("Chưa có hội thoại", SwingConstants.CENTER);
    private final JPanel center = new JPanel(new BorderLayout());
    private boolean unreadOnly;
    private Consumer<ConversationSummary> selectionListener = ignored -> { };
    private Runnable findPeopleListener = () -> { };
    private Runnable createGroupListener = () -> { };
    private Runnable manageGroupListener = () -> { };

    public ConversationListPanel() {
        super(new BorderLayout());
        setBackground(ChatTheme.MILK);
        setPreferredSize(new Dimension(300, 600));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));

        JPanel top = new JPanel();
        top.setBackground(ChatTheme.MILK);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(BorderFactory.createEmptyBorder(24, 20, 10, 20));

        JLabel heading = new JLabel("Messages");
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        heading.setForeground(INK);
        JPanel headingRow = new JPanel(new BorderLayout());
        headingRow.setOpaque(false);
        headingRow.setAlignmentX(LEFT_ALIGNMENT);
        headingRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        headingRow.add(heading, BorderLayout.WEST);
        actionsButton.setText("+");
        actionsButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        actionsButton.setForeground(Color.WHITE);
        actionsButton.setBackground(BLUE);
        actionsButton.setFocusPainted(false);
        actionsButton.setToolTipText("Mở các chức năng khác");
        actionsButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        actionsButton.setPreferredSize(new Dimension(36, 36));
        actionsButton.setBorder(BorderFactory.createEmptyBorder());
        JPopupMenu actions = new JPopupMenu();
        JMenuItem findPeople = new JMenuItem("Tìm người");
        JMenuItem createGroup = new JMenuItem("Tạo nhóm chat");
        JMenuItem manageGroup = new JMenuItem("Quản lý nhóm chat");
        findPeople.addActionListener(event -> findPeopleListener.run());
        createGroup.addActionListener(event -> createGroupListener.run());
        manageGroup.addActionListener(event -> manageGroupListener.run());
        actions.add(findPeople);
        actions.add(createGroup);
        actions.add(manageGroup);
        actionsButton.addActionListener(event -> actions.show(actionsButton, 0, actionsButton.getHeight()));
        headingRow.add(actionsButton, BorderLayout.EAST);
        top.add(headingRow);
        top.add(Box.createVerticalStrut(18));

        JPanel searchBox = new RoundedPanel(new Color(235, 245, 236), 18);
        searchBox.setLayout(new BorderLayout(10, 0));
        searchBox.setBorder(BorderFactory.createEmptyBorder(0, 13, 0, 13));
        searchBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        searchBox.setPreferredSize(new Dimension(260, 42));
        searchBox.setAlignmentX(LEFT_ALIGNMENT);
        searchBox.add(new SearchGlyph(), BorderLayout.WEST);
        search.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        search.setOpaque(false);
        search.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        search.setForeground(INK);
        search.setToolTipText("Tìm theo tên hoặc nội dung tin nhắn");
        search.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                search.repaint();
            }

            @Override
            public void focusLost(FocusEvent event) {
                search.repaint();
            }
        });
        searchBox.add(search, BorderLayout.CENTER);
        top.add(searchBox);
        top.add(Box.createVerticalStrut(15));
        JLabel recent = new JLabel("RECENT");
        recent.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        recent.setForeground(MUTED);
        recent.setAlignmentX(LEFT_ALIGNMENT);
        top.add(recent);
        top.add(Box.createVerticalStrut(5));
        JMenuItem toggleUnread = new JMenuItem("Chỉ chưa đọc");
        toggleUnread.addActionListener(event -> { unreadOnly = !unreadOnly; refresh(); });
        actions.add(toggleUnread);
        add(top, BorderLayout.NORTH);

        list.setBackground(ChatTheme.MILK);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new ConversationRenderer());
        list.setFixedCellHeight(68);
        list.setBorder(BorderFactory.createEmptyBorder(2, 8, 8, 8));
        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) selectionListener.accept(list.getSelectedValue());
        });
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ChatTheme.MILK);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        center.setBackground(ChatTheme.MILK);
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

    public void setFindPeopleListener(Runnable listener) {
        findPeopleListener = listener == null ? () -> { } : listener;
    }

    public void setCreateGroupListener(Runnable listener) {
        createGroupListener = listener == null ? () -> { } : listener;
    }

    public void setManageGroupListener(Runnable listener) {
        manageGroupListener = listener == null ? () -> { } : listener;
    }

    public void clearSelection() {
        list.clearSelection();
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

    private static Color avatarColor(String title, boolean group) {
        if (group) return new Color(193, 166, 122);
        char first = Character.toUpperCase(title.strip().charAt(0));
        return switch (first) {
            case 'A', 'N' -> new Color(60, 171, 161);
            case 'M' -> new Color(204, 169, 121);
            case 'T' -> new Color(92, 176, 164);
            case 'H' -> new Color(116, 172, 165);
            default -> new Color(75, 162, 153);
        };
    }

    /** Compact initials avatar shared with the chat header. */
    static final class Avatar extends JPanel {
        private final String initials;
        private final boolean group;
        private final Color color;

        Avatar(String title, boolean group, int size) {
            this.group = group;
            this.color = avatarColor(title, group);
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
            g.setColor(color);
            int size = Math.min(getWidth(), getHeight());
            g.fillOval(0, 0, size, size);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(11, size / 3)));
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
            setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            avatarSlot.setOpaque(false);
            avatarSlot.setPreferredSize(new Dimension(40, 40));
            add(avatarSlot, BorderLayout.WEST);
            JPanel texts = new JPanel(new BorderLayout(0, 5));
            texts.setOpaque(false);
            name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            name.setForeground(INK);
            snippet.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            snippet.setForeground(MUTED);
            texts.add(name, BorderLayout.NORTH);
            texts.add(snippet, BorderLayout.CENTER);
            add(texts, BorderLayout.CENTER);
            JPanel meta = new JPanel();
            meta.setOpaque(false);
            meta.setLayout(new BoxLayout(meta, BoxLayout.Y_AXIS));
            timestamp.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
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
            avatarSlot.add(new Avatar(title, item.conversation().type() == ConversationType.GROUP, 40),
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
            g.setColor(selected ? PALE_BLUE : ChatTheme.MILK);
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

    private static final class RoundedActionButton extends JButton {
        RoundedActionButton() {
            setContentAreaFilled(false);
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getModel().isRollover() ? ChatTheme.TEAL_DARK : ChatTheme.TEAL);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
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
            int y = (getHeight() - 19) / 2;
            g.drawOval(2, y + 1, 10, 10);
            g.drawLine(11, y + 10, 17, y + 16);
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
            setForeground(active ? Color.WHITE : MUTED);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(active ? BLUE : Color.WHITE);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
