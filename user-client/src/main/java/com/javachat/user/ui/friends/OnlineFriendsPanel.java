package com.javachat.user.ui.friends;

import com.javachat.shared.dto.AuthDtos.PresenceChangedEvent;
import com.javachat.shared.dto.FriendDtos.FriendListResponse;
import com.javachat.shared.model.PresenceStatus;
import com.javachat.shared.model.User;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Friends who are currently online, kept up to date by USER_PRESENCE_CHANGED_EVENT. */
public final class OnlineFriendsPanel extends JPanel {
    private static final Color INK = new Color(30, 48, 72);
    private static final Color MUTED = new Color(113, 132, 157);
    private static final Color BLUE = new Color(45, 112, 220);
    private static final Color GREEN = new Color(46, 175, 98);
    private static final Color CANVAS = new Color(238, 245, 252);

    private final List<User> friends = new ArrayList<>();
    /** Time each online user came online; absence means offline. */
    private final Map<Long, Instant> onlineSince = new HashMap<>();
    private final DefaultListModel<User> model = new DefaultListModel<>();
    private final JList<User> list = new JList<>(model);
    private final JScrollPane scroll = new JScrollPane(list);
    private final JPanel results = new JPanel(new BorderLayout());
    private final JLabel empty = new JLabel("", SwingConstants.CENTER);
    private final JLabel count = new JLabel();
    private final JButton message = new JButton("Nhắn tin");
    private Consumer<User> openConversation = ignored -> { };
    private boolean canOpenConversation;

    public OnlineFriendsPanel() {
        super(new BorderLayout(0, 20));
        setBackground(CANVAS);
        setBorder(BorderFactory.createEmptyBorder(30, 38, 30, 38));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Bạn bè đang online");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        title.setForeground(INK);
        count.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        count.setForeground(MUTED);
        header.add(title, BorderLayout.WEST);
        header.add(count, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new OnlineRenderer());
        list.setFixedCellHeight(72);
        list.setBackground(Color.WHITE);
        list.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) updateMessageButton();
        });
        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && canOpenConversation) openSelectedFriend();
            }
        });
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        results.setBackground(Color.WHITE);
        results.setBorder(BorderFactory.createLineBorder(new Color(226, 235, 245)));
        add(results, BorderLayout.CENTER);
        empty.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        empty.setForeground(MUTED);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        message.setBackground(BLUE);
        message.setForeground(Color.WHITE);
        message.setFocusPainted(false);
        message.setEnabled(false);
        message.addActionListener(event -> openSelectedFriend());
        actions.add(message);
        add(actions, BorderLayout.SOUTH);
        refresh();
    }

    /** Update on the Swing event dispatch thread after FRIEND_LIST_RESPONSE. */
    public void setFriendList(FriendListResponse response) {
        friends.clear();
        if (response != null) {
            friends.addAll(response.friends().stream().filter(user -> user != null).toList());
        }
        refresh();
    }

    /** Replace the known online users, e.g. from an initial presence snapshot. */
    public void setOnlineUserIds(Collection<Long> userIds) {
        onlineSince.clear();
        Instant now = Instant.now();
        if (userIds != null) {
            for (Long id : userIds) if (id != null) onlineSince.put(id, now);
        }
        refresh();
    }

    /** Apply USER_PRESENCE_CHANGED_EVENT on the Swing event dispatch thread. */
    public void applyPresence(PresenceChangedEvent event) {
        if (event == null || event.status() == null) return;
        if (event.status() == PresenceStatus.ONLINE) {
            onlineSince.put(event.userId(),
                    event.changedAt() == null ? Instant.now() : event.changedAt());
        } else {
            onlineSince.remove(event.userId());
        }
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
        model.clear();
        friends.stream()
                .filter(friend -> onlineSince.containsKey(friend.id()))
                .sorted(Comparator.comparing(friend -> name(friend).toLowerCase(Locale.ROOT)))
                .forEach(model::addElement);
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i).id() == selectedId) list.setSelectedIndex(i);
        }
        count.setText(model.size() + " / " + friends.size() + " người bạn đang online");
        results.removeAll();
        if (model.isEmpty()) {
            empty.setText(friends.isEmpty() ? "Chưa có bạn bè" : "Không có bạn bè nào đang online");
            results.add(empty, BorderLayout.CENTER);
        } else {
            results.add(scroll, BorderLayout.CENTER);
        }
        updateMessageButton();
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

    private static String since(Instant start) {
        if (start == null) return "Đang online";
        long minutes = Duration.between(start, Instant.now()).toMinutes();
        if (minutes < 1) return "Vừa online";
        if (minutes < 60) return "Online " + minutes + " phút";
        return "Online " + (minutes / 60) + " giờ";
    }

    private final class OnlineRenderer extends JPanel implements ListCellRenderer<User> {
        private final JLabel avatar = new JLabel("", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = 13;
                int x = getWidth() - size;
                int y = getHeight() - size;
                g.setColor(Color.WHITE);
                g.fillOval(x - 1, y - 1, size + 2, size + 2);
                g.setColor(GREEN);
                g.fillOval(x + 1, y + 1, size - 2, size - 2);
                g.dispose();
            }
        };
        private final JLabel name = new JLabel();
        private final JLabel status = new JLabel();
        private boolean selected;

        OnlineRenderer() {
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
            status.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            status.setForeground(GREEN);
            labels.add(name, BorderLayout.NORTH);
            labels.add(status, BorderLayout.CENTER);
            add(labels, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends User> source, User user,
                int index, boolean isSelected, boolean focused) {
            selected = isSelected;
            String displayName = name(user);
            avatar.setText(displayName.substring(0, 1).toUpperCase(Locale.ROOT));
            name.setText(displayName);
            String username = user.username() == null ? "" : "@" + user.username() + " · ";
            status.setText(username + since(onlineSince.get(user.id())));
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(selected ? new Color(231, 241, 253) : Color.WHITE);
            g.fillRoundRect(0, 2, getWidth(), getHeight() - 4, 14, 14);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
