package com.javachat.user.ui.friends;

import com.javachat.user.ui.chat.ChatTheme;

import com.javachat.shared.dto.FriendDtos.FriendListResponse;
import com.javachat.shared.model.User;

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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Friend list view; data and actions are supplied by the application layer. */
public final class FriendListPanel extends JPanel {
    private static final Color INK = ChatTheme.INK;
    private static final Color MUTED = ChatTheme.MUTED;
    private static final Color BLUE = ChatTheme.TEAL;
    private static final Color CANVAS = ChatTheme.CREAM;

    private final List<User> friends = new ArrayList<>();
    private final DefaultListModel<User> model = new DefaultListModel<>();
    private final JList<User> list = new JList<>(model);
    private final JScrollPane scroll = new JScrollPane(list);
    private final JTextField search = new JTextField() {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!getText().isEmpty() || hasFocus()) return;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setColor(MUTED);
            g.setFont(getFont());
            var metrics = g.getFontMetrics();
            g.drawString("Tìm theo tên hoặc username", 12,
                    (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
        }
    };
    private final JLabel count = new JLabel();
    private final JLabel empty = new JLabel("Chưa có bạn bè", SwingConstants.CENTER);
    private final JPanel results = new JPanel(new BorderLayout());
    private final JButton message = new JButton("Nhắn tin");
    private Consumer<User> openConversation = ignored -> { };
    private boolean canOpenConversation;

    public FriendListPanel() {
        super(new BorderLayout(0, 20));
        setBackground(CANVAS);
        setBorder(BorderFactory.createEmptyBorder(30, 38, 30, 38));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel("Bạn bè");
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        heading.setForeground(INK);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        header.add(heading);
        header.add(Box.createVerticalStrut(5));
        count.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        count.setForeground(MUTED);
        count.setAlignmentX(LEFT_ALIGNMENT);
        header.add(count);
        header.add(Box.createVerticalStrut(20));
        search.setPreferredSize(new Dimension(400, 42));
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        search.setAlignmentX(LEFT_ALIGNMENT);
        search.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(232, 236, 242)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        search.putClientProperty("JTextField.placeholderText", "Tìm theo tên hoặc username");
        search.setToolTipText("Tìm theo tên hoặc username");
        header.add(search);
        add(header, BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new FriendRenderer());
        list.setFixedCellHeight(72);
        list.setBackground(Color.WHITE);
        list.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) updateMessageButton();
        });
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2 && canOpenConversation && list.getSelectedValue() != null) {
                    openSelectedFriend();
                }
            }
        });
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        results.setBackground(Color.WHITE);
        results.setBorder(BorderFactory.createLineBorder(new Color(226, 235, 245)));
        results.add(scroll, BorderLayout.CENTER);
        add(results, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        message.setBackground(BLUE);
        message.setForeground(Color.WHITE);
        message.setFocusPainted(false);
        message.setEnabled(false);
        message.addActionListener(event -> openSelectedFriend());
        actions.add(message);
        add(actions, BorderLayout.SOUTH);

        empty.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        empty.setForeground(MUTED);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { refresh(); }
            public void removeUpdate(DocumentEvent event) { refresh(); }
            public void changedUpdate(DocumentEvent event) { refresh(); }
        });
        refresh();
    }

    /** Update on the Swing event dispatch thread after FRIEND_LIST_RESPONSE. */
    public void setFriendList(FriendListResponse response) {
        setFriends(response == null ? List.of() : response.friends());
    }

    public void setFriends(List<User> users) {
        friends.clear();
        if (users != null) friends.addAll(users.stream().filter(user -> user != null).toList());
        friends.sort(Comparator.comparing(user -> normalized(name(user))));
        refresh();
    }

    public void setOpenConversationListener(Consumer<User> listener) {
        openConversation = listener == null ? ignored -> { } : listener;
        canOpenConversation = listener != null;
        updateMessageButton();
    }

    private void openSelectedFriend() {
        User selected = list.getSelectedValue();
        if (selected != null) openConversation.accept(selected);
    }

    private void refresh() {
        long selectedId = list.getSelectedValue() == null ? -1 : list.getSelectedValue().id();
        String query = normalized(search.getText().strip());
        model.clear();
        for (User friend : friends) {
            if (normalized(name(friend)).contains(query) || normalized(friend.username()).contains(query)) {
                model.addElement(friend);
            }
        }
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i).id() == selectedId) list.setSelectedIndex(i);
        }
        count.setText(friends.size() + " người bạn");
        updateMessageButton();
        results.removeAll();
        if (model.isEmpty()) {
            empty.setText(friends.isEmpty() ? "Chưa có bạn bè" : "Không tìm thấy bạn bè");
            results.add(empty, BorderLayout.CENTER);
        } else {
            results.add(scroll, BorderLayout.CENTER);
        }
        results.revalidate();
        results.repaint();
    }

    private void updateMessageButton() {
        message.setEnabled(canOpenConversation && list.getSelectedValue() != null);
    }

    private static String name(User user) {
        if (user.displayName() != null && !user.displayName().isBlank()) return user.displayName();
        if (user.username() != null && !user.username().isBlank()) return user.username();
        return "Người dùng " + user.id();
    }

    private static String normalized(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    private static final class FriendRenderer extends JPanel implements ListCellRenderer<User> {
        private final JLabel avatar = new JLabel("", SwingConstants.CENTER);
        private final JLabel name = new JLabel();
        private final JLabel username = new JLabel();
        private boolean selected;

        FriendRenderer() {
            super(new BorderLayout(14, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
            avatar.setPreferredSize(new Dimension(42, 42));
            avatar.setOpaque(true);
            avatar.setBackground(new Color(219, 234, 252));
            avatar.setForeground(BLUE);
            avatar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            add(avatar, BorderLayout.WEST);
            JPanel labels = new JPanel(new BorderLayout(0, 3));
            labels.setOpaque(false);
            name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            name.setForeground(INK);
            username.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            username.setForeground(MUTED);
            labels.add(name, BorderLayout.NORTH);
            labels.add(username, BorderLayout.CENTER);
            add(labels, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends User> source, User user,
                int index, boolean isSelected, boolean focused) {
            selected = isSelected;
            String displayName = name(user);
            avatar.setText(displayName.substring(0, 1).toUpperCase(Locale.ROOT));
            name.setText(displayName);
            username.setText(user.username() == null ? "" : "@" + user.username());
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(selected ? new Color(237, 247, 255) : Color.WHITE);
            g.fillRoundRect(0, 2, getWidth(), getHeight() - 4, 14, 14);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
