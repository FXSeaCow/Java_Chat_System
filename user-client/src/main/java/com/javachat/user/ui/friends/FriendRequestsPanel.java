package com.javachat.user.ui.friends;

import com.javachat.user.ui.chat.ChatTheme;

import com.javachat.shared.dto.FriendDtos.FriendListResponse;
import com.javachat.shared.dto.FriendDtos.FriendRequestDecision;
import com.javachat.shared.dto.FriendDtos.RespondRequest;
import com.javachat.shared.model.Friendship;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Incoming and outgoing friend requests backed by FRIEND_LIST_RESPONSE. */
public final class FriendRequestsPanel extends JPanel {
    private static final Color INK = ChatTheme.INK;
    private static final Color MUTED = ChatTheme.MUTED;
    private static final Color BLUE = ChatTheme.TEAL;
    private static final Color CANVAS = ChatTheme.CREAM;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final List<Friendship> incoming = new ArrayList<>();
    private final List<Friendship> outgoing = new ArrayList<>();
    private final Map<Long, User> users = new HashMap<>();
    private final DefaultListModel<Friendship> model = new DefaultListModel<>();
    private final JList<Friendship> list = new JList<>(model);
    private final JScrollPane scroll = new JScrollPane(list);
    private final JPanel results = new JPanel(new BorderLayout());
    private final JLabel empty = new JLabel("", SwingConstants.CENTER);
    private final JLabel count = new JLabel();
    private final JButton incomingButton = new JButton();
    private final JButton outgoingButton = new JButton();
    private final JButton acceptButton = new JButton("Chấp nhận");
    private final JButton rejectButton = new JButton("Từ chối");
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
    private boolean showIncoming = true;
    private long pendingRequestId = -1;
    private Consumer<RespondRequest> respondListener;

    public FriendRequestsPanel() {
        super(new BorderLayout(0, 20));
        setBackground(CANVAS);
        setBorder(BorderFactory.createEmptyBorder(30, 38, 30, 38));

        JPanel header = new JPanel(new BorderLayout(0, 18));
        header.setOpaque(false);
        JPanel headingRow = new JPanel(new BorderLayout());
        headingRow.setOpaque(false);
        JLabel title = new JLabel("Yêu cầu kết bạn");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        title.setForeground(INK);
        count.setForeground(MUTED);
        count.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        headingRow.add(title, BorderLayout.WEST);
        headingRow.add(count, BorderLayout.EAST);
        header.add(headingRow, BorderLayout.NORTH);
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tabs.setOpaque(false);
        incomingButton.addActionListener(event -> { showIncoming = true; refresh(); });
        outgoingButton.addActionListener(event -> { showIncoming = false; refresh(); });
        tabs.add(incomingButton);
        tabs.add(outgoingButton);
        header.add(tabs, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(76);
        list.setCellRenderer(new RequestRenderer());
        list.setBackground(Color.WHITE);
        list.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) updateActions();
        });
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        results.setBackground(Color.WHITE);
        results.setBorder(BorderFactory.createLineBorder(new Color(226, 235, 245)));
        add(results, BorderLayout.CENTER);

        empty.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        empty.setForeground(MUTED);
        actions.setOpaque(false);
        acceptButton.setBackground(BLUE);
        acceptButton.setForeground(Color.WHITE);
        acceptButton.setFocusPainted(false);
        rejectButton.setFocusPainted(false);
        acceptButton.addActionListener(event -> respond(FriendRequestDecision.ACCEPT));
        rejectButton.addActionListener(event -> respond(FriendRequestDecision.REJECT));
        actions.add(rejectButton);
        actions.add(acceptButton);
        add(actions, BorderLayout.SOUTH);
        refresh();
    }

    /** Update on the Swing event dispatch thread after FRIEND_LIST_RESPONSE. */
    public void setFriendList(FriendListResponse response) {
        incoming.clear();
        outgoing.clear();
        if (response != null) {
            incoming.addAll(response.incomingRequests().stream().filter(item -> item != null).toList());
            outgoing.addAll(response.outgoingRequests().stream().filter(item -> item != null).toList());
        }
        Comparator<Friendship> newestFirst = Comparator.comparing(Friendship::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder()));
        incoming.sort(newestFirst);
        outgoing.sort(newestFirst);
        pendingRequestId = -1;
        refresh();
    }

    /** Optional user profiles to show names instead of IDs in request rows. */
    public void setRequestUsers(List<User> profiles) {
        users.clear();
        if (profiles != null) {
            for (User user : profiles) if (user != null) users.put(user.id(), user);
        }
        list.repaint();
    }

    public void setRespondRequestListener(Consumer<RespondRequest> listener) {
        respondListener = listener;
        updateActions();
    }

    /** Re-enable a request after the application reports a failed response. */
    public void setResponseError(long friendshipId) {
        if (pendingRequestId == friendshipId) {
            pendingRequestId = -1;
            updateActions();
        }
    }

    private void respond(FriendRequestDecision decision) {
        Friendship selected = list.getSelectedValue();
        if (selected == null || !showIncoming || respondListener == null || pendingRequestId != -1) return;
        pendingRequestId = selected.id();
        updateActions();
        respondListener.accept(new RespondRequest(selected.id(), decision));
    }

    private void refresh() {
        long selectedId = list.getSelectedValue() == null ? -1 : list.getSelectedValue().id();
        model.clear();
        for (Friendship request : showIncoming ? incoming : outgoing) model.addElement(request);
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i).id() == selectedId) list.setSelectedIndex(i);
        }
        incomingButton.setText("Đã nhận (" + incoming.size() + ")");
        outgoingButton.setText("Đã gửi (" + outgoing.size() + ")");
        incomingButton.setEnabled(!showIncoming);
        outgoingButton.setEnabled(showIncoming);
        count.setText(model.size() + " lời mời");
        results.removeAll();
        if (model.isEmpty()) {
            empty.setText(showIncoming ? "Chưa có lời mời kết bạn" : "Chưa gửi lời mời kết bạn");
            results.add(empty, BorderLayout.CENTER);
        } else {
            results.add(scroll, BorderLayout.CENTER);
        }
        updateActions();
        results.revalidate();
        results.repaint();
    }

    private void updateActions() {
        actions.setVisible(showIncoming);
        boolean enabled = showIncoming && respondListener != null && pendingRequestId == -1
                && list.getSelectedValue() != null;
        acceptButton.setEnabled(enabled);
        rejectButton.setEnabled(enabled);
    }

    private String displayName(long userId) {
        User user = users.get(userId);
        if (user == null) return "Người dùng #" + userId;
        if (user.displayName() != null && !user.displayName().isBlank()) return user.displayName();
        return user.username() == null || user.username().isBlank()
                ? "Người dùng #" + userId : user.username();
    }

    private final class RequestRenderer extends JPanel implements ListCellRenderer<Friendship> {
        private final JLabel avatar = new JLabel("", SwingConstants.CENTER);
        private final JLabel name = new JLabel();
        private final JLabel description = new JLabel();
        private boolean selected;

        RequestRenderer() {
            super(new BorderLayout(14, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
            avatar.setPreferredSize(new Dimension(44, 44));
            avatar.setOpaque(true);
            avatar.setBackground(new Color(219, 234, 252));
            avatar.setForeground(BLUE);
            avatar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            add(avatar, BorderLayout.WEST);
            JPanel labels = new JPanel(new BorderLayout(0, 4));
            labels.setOpaque(false);
            name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            name.setForeground(INK);
            description.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            description.setForeground(MUTED);
            labels.add(name, BorderLayout.NORTH);
            labels.add(description, BorderLayout.CENTER);
            add(labels, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Friendship> source,
                Friendship request, int index, boolean isSelected, boolean focused) {
            selected = isSelected;
            long otherId = showIncoming ? request.requesterId() : request.addresseeId();
            String display = displayName(otherId);
            avatar.setText(display.substring(0, 1).toUpperCase());
            name.setText(display);
            String date = request.createdAt() == null ? ""
                    : " · " + DATE.format(request.createdAt().atZone(ZoneId.systemDefault()));
            description.setText((showIncoming ? "Muốn kết bạn" : "Đang chờ phản hồi") + date);
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
