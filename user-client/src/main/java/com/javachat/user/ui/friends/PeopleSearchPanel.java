package com.javachat.user.ui.friends;

import com.javachat.user.ui.chat.ChatTheme;

import com.javachat.shared.dto.FriendDtos.SendRequest;
import com.javachat.shared.model.User;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/** UI-only people finder. Network search and friend requests are supplied through callbacks. */
public final class PeopleSearchPanel extends JPanel {
    private static final Color INK = ChatTheme.INK;
    private static final Color MUTED = ChatTheme.MUTED;
    private static final Color BLUE = ChatTheme.TEAL;
    private static final Color BORDER = ChatTheme.BORDER;

    private final List<User> people = new ArrayList<>();
    private final Set<Long> friends = new HashSet<>();
    private final Set<Long> pending = new HashSet<>();
    private final DefaultListModel<User> model = new DefaultListModel<>();
    private final JList<User> results = new JList<>(model);
    private final JTextField search = new JTextField();
    private final JLabel status = new JLabel("Nhập tên hoặc tên đăng nhập để tìm kiếm", SwingConstants.CENTER);
    private Consumer<String> searchListener = ignored -> { };
    private Consumer<SendRequest> addFriendListener = ignored -> { };

    public PeopleSearchPanel() {
        super(new BorderLayout());
        setBackground(new Color(239, 240, 243));
        setBorder(BorderFactory.createEmptyBorder(38, 54, 38, 54));

        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(30, 34, 30, 34)));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Tìm người");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        title.setForeground(INK);
        title.setAlignmentX(LEFT_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(7));
        JLabel hint = new JLabel("Tìm bạn bè mới trên Java Chat");
        hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        hint.setForeground(MUTED);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        header.add(hint);
        header.add(Box.createVerticalStrut(22));
        JPanel searchRow = new JPanel(new BorderLayout(10, 0));
        searchRow.setOpaque(false);
        search.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        search.setToolTipText("Tìm theo tên hiển thị hoặc tên đăng nhập");
        search.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(11, 13, 11, 13)));
        JButton searchButton = button("Tìm kiếm");
        searchButton.addActionListener(event -> searchListener.accept(search.getText().strip()));
        search.addActionListener(event -> searchButton.doClick());
        searchRow.add(search, BorderLayout.CENTER);
        searchRow.add(searchButton, BorderLayout.EAST);
        searchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        searchRow.setAlignmentX(LEFT_ALIGNMENT);
        header.add(searchRow);
        content.add(header, BorderLayout.NORTH);

        results.setCellRenderer(new PersonRenderer());
        results.setFixedCellHeight(76);
        results.setBackground(Color.WHITE);
        results.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent event) {
                int index = results.locationToIndex(event.getPoint());
                if (index < 0 || event.getX() < results.getWidth() - 145) return;
                User user = model.get(index);
                if (!friends.contains(user.id()) && !pending.contains(user.id())) {
                    pending.add(user.id());
                    addFriendListener.accept(new SendRequest(user.id()));
                    results.repaint();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(results);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        content.add(scroll, BorderLayout.CENTER);
        status.setForeground(MUTED);
        status.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        content.add(status, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);

        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { filter(); }
            public void removeUpdate(DocumentEvent event) { filter(); }
            public void changedUpdate(DocumentEvent event) { filter(); }
        });
        filter();
    }

    public void setPeople(List<User> users) {
        people.clear();
        if (users != null) people.addAll(users.stream().filter(user -> user != null).toList());
        filter();
    }

    public void setFriendIds(Set<Long> ids) { replace(friends, ids); filter(); }
    public void setPendingIds(Set<Long> ids) { replace(pending, ids); filter(); }
    public void setSearchListener(Consumer<String> listener) {
        searchListener = listener == null ? ignored -> { } : listener;
    }
    public void setAddFriendListener(Consumer<SendRequest> listener) {
        addFriendListener = listener == null ? ignored -> { } : listener;
    }
    public void markRequestSent(long userId) { pending.add(userId); results.repaint(); }
    public void focusSearch() { search.requestFocusInWindow(); }

    private static void replace(Set<Long> target, Set<Long> values) {
        target.clear();
        if (values != null) target.addAll(values);
    }

    private void filter() {
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        model.clear();
        if (!query.isEmpty()) {
            for (User user : people) {
                String displayName = user.displayName() == null ? "" : user.displayName();
                String username = user.username() == null ? "" : user.username();
                if (displayName.toLowerCase(Locale.ROOT).contains(query)
                        || username.toLowerCase(Locale.ROOT).contains(query)) model.addElement(user);
            }
        }
        status.setText(query.isEmpty() ? "Nhập tên hoặc tên đăng nhập để tìm kiếm"
                : model.isEmpty() ? "Không tìm thấy người dùng phù hợp"
                : "Tìm thấy " + model.size() + " người dùng");
    }

    private static JButton button(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(BLUE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private final class PersonRenderer extends JPanel implements ListCellRenderer<User> {
        private final Avatar avatar = new Avatar();
        private final JLabel name = new JLabel();
        private final JLabel username = new JLabel();
        private final JButton action = new JButton();
        private User user;

        PersonRenderer() {
            super(new BorderLayout(14, 0));
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
            setBackground(Color.WHITE);
            add(avatar, BorderLayout.WEST);
            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            name.setForeground(INK);
            username.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            username.setForeground(MUTED);
            text.add(Box.createVerticalGlue()); text.add(name); text.add(Box.createVerticalStrut(5));
            text.add(username); text.add(Box.createVerticalGlue());
            add(text, BorderLayout.CENTER);
            JPanel slot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 20));
            slot.setOpaque(false);
            action.setFocusPainted(false);
            action.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            action.addActionListener(event -> sendRequest());
            slot.add(action);
            add(slot, BorderLayout.EAST);
        }

        private void sendRequest() {
            if (user != null && !friends.contains(user.id()) && !pending.contains(user.id())) {
                pending.add(user.id());
                addFriendListener.accept(new SendRequest(user.id()));
                results.repaint();
            }
        }

        public Component getListCellRendererComponent(JList<? extends User> list, User value, int index,
                boolean selected, boolean focused) {
            user = value;
            avatar.setUser(value);
            name.setText(value.displayName() == null || value.displayName().isBlank() ? value.username() : value.displayName());
            username.setText("@" + value.username());
            boolean isFriend = friends.contains(value.id());
            boolean isPending = pending.contains(value.id());
            action.setText(isFriend ? "Bạn bè" : isPending ? "Đã gửi lời mời" : "+ Kết bạn");
            action.setEnabled(!isFriend && !isPending);
            action.setForeground(isFriend || isPending ? MUTED : BLUE);
            action.setBackground(isFriend || isPending ? new Color(242, 245, 248) : new Color(237, 247, 255));
            action.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            return this;
        }
    }

    private static final class Avatar extends JPanel {
        private String initial = "?";
        Avatar() { setOpaque(false); setPreferredSize(new Dimension(48, 48)); }
        void setUser(User user) {
            String value = user.displayName() == null || user.displayName().isBlank() ? user.username() : user.displayName();
            initial = value == null || value.isBlank() ? "?" : value.substring(0, 1).toUpperCase(Locale.ROOT);
        }
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(219, 234, 252)); g.fillOval(0, 14, 48, 48);
            g.setColor(BLUE); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            var metrics = g.getFontMetrics();
            g.drawString(initial, (48 - metrics.stringWidth(initial)) / 2,
                    14 + (48 - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
        }
    }
}
