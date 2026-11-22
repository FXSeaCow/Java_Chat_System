package com.javachat.user.ui.chat;

import com.javachat.shared.dto.ChatDtos.ConversationSummary;
import com.javachat.shared.dto.ChatDtos.MessageSendRequest;
import com.javachat.shared.dto.AiDtos;
import com.javachat.shared.model.ChatMessage;
import com.javachat.shared.dto.AuthDtos.PresenceChangedEvent;
import com.javachat.shared.dto.FriendDtos.FriendListResponse;
import com.javachat.shared.dto.FriendDtos.RespondRequest;
import com.javachat.user.ui.friends.FriendListPanel;
import com.javachat.user.ui.friends.FriendRequestsPanel;
import com.javachat.user.ui.friends.OnlineFriendsPanel;
import com.javachat.shared.model.User;
import com.javachat.shared.dto.FriendDtos.SendRequest;
import com.javachat.user.ui.friends.PeopleSearchPanel;
import com.javachat.user.ui.groups.GroupCreatePanel;
import com.javachat.user.ui.groups.GroupManagementPanel;
import com.javachat.shared.dto.GroupDtos.GroupCreateRequest;
import com.javachat.shared.dto.GroupDtos.GroupUpdateRequest;
import com.javachat.shared.dto.GroupDtos.MemberRemoveRequest;
import com.javachat.shared.dto.GroupDtos.MemberRoleUpdateRequest;
import com.javachat.shared.model.ConversationMember;
import com.javachat.shared.model.GroupDetails;
import com.javachat.shared.dto.AuthDtos.ProfileUpdateRequest;
import com.javachat.shared.dto.AuthDtos.ChangePasswordRequest;
import com.javachat.user.ui.account.ProfilePanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Main chat shell: conversation list on the left, chat panel on the right. */
public final class MainFrame extends JFrame {
    private static final Color INK = ChatTheme.INK;
    private static final Color MUTED = ChatTheme.MUTED;
    private static final Color BLUE = ChatTheme.TEAL;
    private static final Color NAVY = ChatTheme.NAVY;
    private static final Color CANVAS = ChatTheme.CREAM;
    private static final int RAIL_COLLAPSED = 76;
    private static final int RAIL_EXPANDED = 220;

    private final ConversationListPanel conversations = new ConversationListPanel();
    private final FriendListPanel friends = new FriendListPanel();
    private final FriendRequestsPanel friendRequests = new FriendRequestsPanel();
    private final OnlineFriendsPanel onlineFriends = new OnlineFriendsPanel();
    private final CardLayout pages = new CardLayout();
    private final JPanel pageContainer = new JPanel(pages);
    private final JPanel detail = new JPanel(new BorderLayout());
    private final ChatPanel chat = new ChatPanel();
    private final PeopleSearchPanel peopleSearch = new PeopleSearchPanel();
    private final GroupCreatePanel groupCreate = new GroupCreatePanel();
    private final GroupManagementPanel groupManagement = new GroupManagementPanel();
    private final ProfilePanel profile = new ProfilePanel();
    private final JPanel navigation = new RailPanel();
    private Timer railAnimation;
    private int railWidth = RAIL_COLLAPSED;
    private JButton chatsButton;
    private JButton friendsButton;
    private JButton onlineButton;
    private JButton requestsButton;
    private JButton settingsButton;
    private JButton accountButton;
    private Consumer<ConversationSummary> openConversation = ignored -> { };

    public MainFrame() {
        super("E-Chat Desktop");
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 740);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) { updateWindowShape(); }
        });
        updateWindowShape();
        setLayout(new BorderLayout());
        getContentPane().setBackground(CANVAS);
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        navigation.setPreferredSize(new Dimension(RAIL_COLLAPSED, 0));
        navigation.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 10, 18, 10));
        JButton logo = railButton("E-Chat", true);
        chatsButton = railButton("Tin nhắn", false);
        friendsButton = railButton("Bạn bè", false);
        onlineButton = railButton("Đang online", false);
        requestsButton = railButton("Lời mời kết bạn", false);
        logo.addActionListener(event -> showChats());
        chatsButton.addActionListener(event -> showChats());
        friendsButton.addActionListener(event -> showFriends());
        onlineButton.addActionListener(event -> showOnlineFriends());
        requestsButton.addActionListener(event -> showFriendRequests());
        navigation.add(logo);
        navigation.add(Box.createVerticalStrut(38));
        navigation.add(chatsButton);
        navigation.add(Box.createVerticalStrut(8));
        navigation.add(friendsButton);
        navigation.add(Box.createVerticalStrut(8));
        navigation.add(onlineButton);
        navigation.add(Box.createVerticalStrut(8));
        navigation.add(requestsButton);
        navigation.add(Box.createVerticalGlue());
        settingsButton = railButton("Quản lý nhóm", false);
        settingsButton.addActionListener(event -> { showChats(); showDetail(groupManagement); setActiveRail(settingsButton); });
        navigation.add(settingsButton);
        navigation.add(Box.createVerticalStrut(8));
        accountButton = railButton("Tài khoản", false);
        accountButton.addActionListener(event -> showAccount());
        navigation.add(accountButton);
        installRailHover(navigation);
        JPanel workspace = new JPanel(new BorderLayout(10, 0));
        workspace.setOpaque(false);
        workspace.add(navigation, BorderLayout.WEST);
        JPanel chatPage = new JPanel(new BorderLayout(10, 0));
        chatPage.setOpaque(false);
        JPanel listCard = new RoundedSurface(ChatTheme.MILK);
        listCard.setPreferredSize(new Dimension(300, 0));
        listCard.add(conversations, BorderLayout.CENTER);
        chatPage.add(listCard, BorderLayout.WEST);
        detail.setBackground(CANVAS);
        JPanel detailCard = new RoundedSurface(ChatTheme.MILK);
        detailCard.add(detail, BorderLayout.CENTER);
        chatPage.add(detailCard, BorderLayout.CENTER);
        pageContainer.add(chatPage, "chats");
        pageContainer.add(wrapPage(friends), "friends");
        pageContainer.add(wrapPage(onlineFriends), "online");
        pageContainer.add(wrapPage(friendRequests), "requests");
        pageContainer.add(wrapPage(profile), "account");
        pageContainer.setBackground(CANVAS);
        workspace.add(pageContainer, BorderLayout.CENTER);
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(CANVAS);
        outer.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 0, 10, 10));
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 20, 12, 0));
        JPanel brandIcon = new BrandMark();
        brandIcon.setPreferredSize(new Dimension(42, 42));
        header.add(brandIcon, BorderLayout.WEST);
        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        JLabel appName = new JLabel("E-Chat Desktop");
        appName.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        appName.setForeground(INK);
        JLabel caption = new JLabel("Chat desktop • redesigned");
        caption.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        caption.setForeground(MUTED);
        brandText.add(appName);
        brandText.add(Box.createVerticalStrut(2));
        brandText.add(caption);
        header.add(brandText, BorderLayout.CENTER);
        JPanel windowControls = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 4, 0));
        windowControls.setOpaque(false);
        JButton minimize = windowButton("−", "Thu nhỏ");
        JButton maximize = windowButton("□", "Phóng to hoặc khôi phục");
        JButton close = windowButton("×", "Đóng ứng dụng");
        minimize.addActionListener(event -> setState(JFrame.ICONIFIED));
        maximize.addActionListener(event -> toggleMaximized());
        close.addActionListener(event -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        windowControls.add(minimize);
        windowControls.add(maximize);
        windowControls.add(close);
        header.add(windowControls, BorderLayout.EAST);
        installWindowDrag(header, brandIcon, brandText, appName, caption);
        outer.add(header, BorderLayout.NORTH);
        outer.add(workspace, BorderLayout.CENTER);
        installWindowResize(outer);
        add(outer, BorderLayout.CENTER);
        setActiveRail(chatsButton);
        showWelcome();
        conversations.setSelectionListener(this::selectConversation);
        conversations.setFindPeopleListener(this::showPeopleSearch);
        conversations.setCreateGroupListener(() -> showDetail(groupCreate));
        conversations.setManageGroupListener(() -> showDetail(groupManagement));
    }

    private static JButton railButton(String tooltip, boolean brand) {
        JButton button = new RailButton(tooltip, brand);
        button.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        button.setPreferredSize(new Dimension(56, 46));
        button.setToolTipText(tooltip);
        button.getAccessibleContext().setAccessibleName(tooltip);
        button.setBackground(brand ? ChatTheme.TEAL : NAVY);
        button.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        return button;
    }

    private static JPanel wrapPage(JPanel page) {
        JPanel surface = new RoundedSurface(ChatTheme.MILK);
        surface.add(page, BorderLayout.CENTER);
        return surface;
    }

    private static JButton windowButton(String symbol, String tooltip) {
        JButton button = new WindowControlButton(symbol, "Đóng ứng dụng".equals(tooltip));
        button.setPreferredSize(new Dimension(38, 36));
        button.setToolTipText(tooltip);
        button.getAccessibleContext().setAccessibleName(tooltip);
        return button;
    }

    private static final class WindowControlButton extends JButton {
        private final boolean close;

        WindowControlButton(String symbol, boolean close) {
            super(symbol);
            this.close = close;
            setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 21));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getModel().isRollover() || getModel().isPressed()) {
                g.setColor(close ? new Color(250, 222, 215) : new Color(225, 239, 233));
                g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 11, 11);
            }
            if (hasFocus()) {
                g.setColor(ChatTheme.TEAL_DARK);
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 10, 10);
            }
            g.setColor(close && getModel().isRollover() ? new Color(169, 53, 43) : INK);
            g.setFont(getFont());
            var metrics = g.getFontMetrics();
            g.drawString(getText(), (getWidth() - metrics.stringWidth(getText())) / 2,
                    (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
        }
    }

    private void toggleMaximized() {
        boolean maximized = (getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0;
        setExtendedState(maximized ? JFrame.NORMAL : JFrame.MAXIMIZED_BOTH);
        SwingUtilities.invokeLater(this::updateWindowShape);
    }

    private void updateWindowShape() {
        try {
            if ((getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0) setShape(null);
            else setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 24, 24));
        } catch (UnsupportedOperationException ignored) {
            // The painted panels remain rounded when native window shaping is unavailable.
        }
    }

    private void installWindowDrag(JPanel header, java.awt.Component... surfaces) {
        MouseAdapter drag = new MouseAdapter() {
            private java.awt.Point origin;
            private java.awt.Point windowOrigin;

            @Override public void mousePressed(MouseEvent event) {
                origin = event.getLocationOnScreen();
                windowOrigin = getLocation();
            }

            @Override public void mouseDragged(MouseEvent event) {
                if (origin == null || (getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0) return;
                java.awt.Point now = event.getLocationOnScreen();
                setLocation(windowOrigin.x + now.x - origin.x, windowOrigin.y + now.y - origin.y);
            }

            @Override public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) toggleMaximized();
            }
        };
        header.addMouseListener(drag);
        header.addMouseMotionListener(drag);
        for (java.awt.Component surface : surfaces) {
            surface.addMouseListener(drag);
            surface.addMouseMotionListener(drag);
        }
    }

    private void installWindowResize(JPanel outer) {
        MouseAdapter resize = new MouseAdapter() {
            private int edge;
            private java.awt.Rectangle startBounds;
            private java.awt.Point startMouse;

            private int edgeAt(MouseEvent event) {
                if ((getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0) return 0;
                java.awt.Point point = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), outer);
                int edge = 0;
                if (point.x < 8) edge |= 1;
                if (point.x > outer.getWidth() - 9) edge |= 2;
                if (point.y < 8) edge |= 4;
                if (point.y > outer.getHeight() - 9) edge |= 8;
                return edge;
            }

            @Override public void mouseMoved(MouseEvent event) {
                int hovered = edgeAt(event);
                int cursor = switch (hovered) {
                    case 1, 2 -> java.awt.Cursor.E_RESIZE_CURSOR;
                    case 4, 8 -> java.awt.Cursor.N_RESIZE_CURSOR;
                    case 5, 10 -> java.awt.Cursor.NW_RESIZE_CURSOR;
                    case 6, 9 -> java.awt.Cursor.NE_RESIZE_CURSOR;
                    default -> java.awt.Cursor.DEFAULT_CURSOR;
                };
                outer.setCursor(java.awt.Cursor.getPredefinedCursor(cursor));
            }

            @Override public void mousePressed(MouseEvent event) {
                edge = edgeAt(event);
                startBounds = getBounds();
                startMouse = event.getLocationOnScreen();
            }

            @Override public void mouseDragged(MouseEvent event) {
                if (edge == 0 || startBounds == null) return;
                java.awt.Point now = event.getLocationOnScreen();
                int dx = now.x - startMouse.x;
                int dy = now.y - startMouse.y;
                int x = startBounds.x;
                int y = startBounds.y;
                int width = startBounds.width;
                int height = startBounds.height;
                if ((edge & 1) != 0) { x += dx; width -= dx; }
                if ((edge & 2) != 0) width += dx;
                if ((edge & 4) != 0) { y += dy; height -= dy; }
                if ((edge & 8) != 0) height += dy;
                Dimension minimum = getMinimumSize();
                if (width < minimum.width) { if ((edge & 1) != 0) x -= minimum.width - width; width = minimum.width; }
                if (height < minimum.height) { if ((edge & 4) != 0) y -= minimum.height - height; height = minimum.height; }
                setBounds(x, y, width, height);
            }
        };
        outer.addMouseListener(resize);
        outer.addMouseMotionListener(resize);
        navigation.addMouseListener(resize);
        navigation.addMouseMotionListener(resize);
    }

    private void installRailHover(JPanel rail) {
        MouseAdapter hover = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent event) { animateRail(true); }
            @Override public void mouseExited(MouseEvent event) {
                java.awt.Point point = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), rail);
                if (!rail.contains(point)) animateRail(false);
            }
        };
        rail.addMouseListener(hover);
        for (java.awt.Component child : rail.getComponents()) child.addMouseListener(hover);
    }

    private void animateRail(boolean expand) {
        int target = expand ? RAIL_EXPANDED : RAIL_COLLAPSED;
        if (railWidth == target) return;
        if (railAnimation != null) railAnimation.stop();
        railAnimation = new Timer(15, event -> {
            int gap = target - railWidth;
            if (Math.abs(gap) <= 2) {
                railWidth = target;
                railAnimation.stop();
            } else {
                railWidth += Math.max(3, Math.abs(gap) / 5) * Integer.signum(gap);
            }
            navigation.setPreferredSize(new Dimension(railWidth, 0));
            navigation.revalidate();
            navigation.repaint();
        });
        railAnimation.start();
    }

    private static final class RailPanel extends JPanel {
        RailPanel() { setOpaque(false); }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(19, 60, 66),
                    0, getHeight(), new Color(28, 99, 100)));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
            g.setColor(new Color(100, 203, 181, 27));
            g.fill(new Ellipse2D.Double(-110, getHeight() * .52, 225, 285));
            g.setColor(new Color(255, 252, 235, 18));
            g.fill(new Ellipse2D.Double(getWidth() - 100, getHeight() - 200, 170, 215));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class RailButton extends JButton {
        private final String label;
        private final boolean brand;

        RailButton(String label, boolean brand) {
            this.label = label;
            this.brand = brand;
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (brand) {
                g.setPaint(new java.awt.GradientPaint(5, 4, new Color(73, 201, 182),
                        46, 42, new Color(32, 149, 145)));
                g.fillRoundRect(3, 2, getWidth() - 6, 42, 14, 14);
            } else if (!NAVY.equals(getBackground()) || getModel().isRollover()) {
                g.setColor(getModel().isRollover() ? new Color(66, 137, 131) : getBackground());
                g.fillRoundRect(2, 1, getWidth() - 4, 44, 13, 13);
            }
            g.setColor(Color.WHITE);
            if (brand) {
                g.fillRoundRect(17, 14, 22, 16, 8, 8);
                g.fillPolygon(new int[] {21, 21, 17}, new int[] {27, 33, 28}, 3);
                g.setColor(ChatTheme.TEAL_DARK);
                for (int x : new int[] {23, 28, 33}) g.fillOval(x, 21, 2, 2);
                g.setColor(Color.WHITE);
            } else {
                paintIcon(g);
            }
            if (getWidth() > 70) {
                float opacity = Math.min(1f, (getWidth() - 70f) / 65f);
                g.setComposite(java.awt.AlphaComposite.SrcOver.derive(opacity));
                g.setFont(new Font(Font.SANS_SERIF, brand ? Font.BOLD : Font.PLAIN, 13));
                g.drawString(label, 58, 28);
            }
            if (hasFocus()) {
                g.setComposite(java.awt.AlphaComposite.SrcOver);
                g.setColor(new Color(218, 255, 242));
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(2, 1, getWidth() - 5, 43, 13, 13);
            }
            g.dispose();
        }

        private void paintIcon(Graphics2D g) {
            g.setStroke(new BasicStroke(2.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (label) {
                case "Tin nhắn" -> {
                    g.drawRoundRect(17, 15, 20, 15, 4, 4);
                    g.drawLine(20, 30, 20, 34);
                    g.drawLine(20, 34, 25, 30);
                    g.drawLine(22, 21, 32, 21);
                }
                case "Bạn bè" -> {
                    g.drawOval(17, 15, 9, 9);
                    g.drawOval(29, 17, 7, 7);
                    g.drawArc(15, 25, 15, 11, 0, 180);
                    g.drawArc(27, 25, 11, 9, 0, 180);
                }
                case "Đang online" -> {
                    g.drawOval(18, 14, 20, 20);
                    g.fillOval(25, 21, 6, 6);
                    g.drawLine(28, 15, 28, 17);
                }
                case "Lời mời kết bạn" -> {
                    g.drawRoundRect(17, 17, 22, 16, 3, 3);
                    g.drawLine(18, 18, 28, 26);
                    g.drawLine(28, 26, 38, 18);
                }
                case "Tài khoản" -> {
                    g.drawOval(18, 13, 20, 20);
                    g.drawOval(24, 17, 8, 8);
                    g.drawArc(21, 26, 14, 10, 20, 140);
                }
                default -> {
                    g.drawOval(19, 16, 18, 18);
                    g.drawOval(24, 21, 8, 8);
                    for (int i = 0; i < 8; i++) {
                        double angle = i * Math.PI / 4;
                        int x1 = 28 + (int) Math.round(Math.cos(angle) * 10);
                        int y1 = 25 + (int) Math.round(Math.sin(angle) * 10);
                        int x2 = 28 + (int) Math.round(Math.cos(angle) * 13);
                        int y2 = 25 + (int) Math.round(Math.sin(angle) * 13);
                        g.drawLine(x1, y1, x2, y2);
                    }
                }
            }
        }
    }

    private static final class BrandMark extends JPanel {
        BrandMark() { setOpaque(false); }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(83, 207, 184),
                    getWidth(), getHeight(), ChatTheme.TEAL_DARK));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g.setColor(Color.WHITE);
            g.fillRoundRect(8, 10, 27, 19, 11, 11);
            g.fillPolygon(new int[] {13, 13, 9}, new int[] {27, 34, 28}, 3);
            g.setColor(ChatTheme.TEAL_DARK);
            for (int x : new int[] {16, 21, 26}) g.fillOval(x, 18, 3, 3);
            g.dispose();
        }
    }

    private static final class RoundedSurface extends JPanel {
        private final Color fill;
        RoundedSurface(Color fill) {
            super(new BorderLayout());
            this.fill = fill;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
            g.setColor(ChatTheme.BORDER);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
            g.dispose();
        }

        @Override protected void paintChildren(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setClip(new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, 25, 25));
            super.paintChildren(g);
            g.dispose();
        }
    }

    /** Supply results from CONVERSATION_LIST_RESPONSE on the Swing event dispatch thread. */
    public void setConversations(List<ConversationSummary> items) {
        conversations.setConversations(items);
    }

    /** Supply FRIEND_LIST_RESPONSE on the Swing event dispatch thread. */
    public void setFriendList(FriendListResponse response) {
        friends.setFriendList(response);
        friendRequests.setFriendList(response);
        onlineFriends.setFriendList(response);
    }

    /** Initial set of online user IDs, if the application has a presence snapshot. */
    public void setOnlineUserIds(java.util.Collection<Long> userIds) {
        onlineFriends.setOnlineUserIds(userIds);
    }

    /** Apply USER_PRESENCE_CHANGED_EVENT on the Swing event dispatch thread. */
    public void applyPresence(PresenceChangedEvent event) {
        onlineFriends.applyPresence(event);
    }

    public void setFriendRequestUsers(List<User> users) {
        friendRequests.setRequestUsers(users);
    }

    public void setRespondFriendRequestListener(Consumer<RespondRequest> listener) {
        friendRequests.setRespondRequestListener(listener);
    }

    public void setFriendRequestResponseError(long friendshipId) {
        friendRequests.setResponseError(friendshipId);
    }

    public void setOpenFriendConversationListener(Consumer<User> listener) {
        friends.setOpenConversationListener(listener);
        onlineFriends.setOpenConversationListener(listener);
    }

    public void showFriends() {
        pages.show(pageContainer, "friends");
        setActiveRail(friendsButton);
    }

    public void showOnlineFriends() {
        pages.show(pageContainer, "online");
        setActiveRail(onlineButton);
    }

    public void showFriendRequests() {
        pages.show(pageContainer, "requests");
        setActiveRail(requestsButton);
    }

    public void showChats() {
        pages.show(pageContainer, "chats");
        if (detail.getComponentCount() == 0 || detail.getComponent(0) != chat) showWelcome();
        setActiveRail(chatsButton);
    }

    public void showAccount() {
        pages.show(pageContainer, "account");
        setActiveRail(accountButton);
    }

    /** Signed-in user's profile shown on the account screen (JC-22). */
    public void setCurrentUser(User user) {
        profile.setUser(user);
    }

    /** Receives ProfileUpdateRequest when the user saves the account form. */
    public void setProfileUpdateListener(Consumer<ProfileUpdateRequest> listener) {
        profile.setUpdateListener(listener);
    }

    public void setLogoutListener(Runnable listener) {
        profile.setLogoutListener(listener);
    }

    public void setLogoutPending(boolean pending) {
        profile.setLogoutPending(pending);
    }

    public void setPasswordChangeListener(Consumer<ChangePasswordRequest> listener) {
        profile.setPasswordChangeListener(listener);
    }

    public void setPasswordChangeFailed(String message) {
        profile.setPasswordChangeFailed(message);
    }

    public void setPasswordChangeSucceeded() {
        profile.setPasswordChangeSucceeded();
    }

    /** Call on the Swing event dispatch thread after the profile update succeeded. */
    public void setProfileUpdateSucceeded(User updated) {
        profile.setSaveSucceeded(updated);
    }

    /** Call on the Swing event dispatch thread when the profile update failed. */
    public void setProfileUpdateFailed(String message) {
        profile.setSaveFailed(message);
    }

    private void setActiveRail(JButton selected) {
        for (JButton button : new JButton[] {chatsButton, friendsButton, onlineButton, requestsButton, settingsButton,
                accountButton}) {
            if (button != null) button.setBackground(button == selected ? new Color(44, 112, 111) : NAVY);
        }
    }

    /** Open an existing direct conversation chosen from the friends screen. */
    public void openConversation(ConversationSummary conversation) {
        if (conversation != null) conversations.selectConversation(conversation.conversation().id());
    }

    public void setOpenConversationListener(Consumer<ConversationSummary> listener) {
        openConversation = listener == null ? ignored -> { } : listener;
    }

    public void setCurrentUserId(long userId) {
        chat.setCurrentUserId(userId);
    }

    /** Called after MESSAGE_HISTORY_RESPONSE for the selected conversation. */
    public void setMessages(long conversationId, List<ChatMessage> messages) {
        if (chat.conversationId() == conversationId) chat.setMessages(messages);
    }

    /** Called after MESSAGE_CREATED_EVENT or a successful send response. */
    public void appendMessage(ChatMessage message) {
        chat.appendMessage(message);
    }

    public void setSendMessageListener(Consumer<MessageSendRequest> listener) {
        chat.setSendListener(listener);
    }

    /** Supply the AI request handler; send the result back on the Swing event dispatch thread. */
    public void setAiSuggestionListener(Consumer<AiDtos.ChatRequest> listener) {
        chat.setAiSuggestionListener(listener);
    }

    public void setAiSuggestionResult(long conversationId, AiDtos.ChatResponse response) {
        chat.setAiSuggestionResult(conversationId, response);
    }

    public void setAiSuggestionError(long conversationId, String message) {
        chat.setAiSuggestionError(conversationId, message);
    }

    public void setPeople(List<User> people) { peopleSearch.setPeople(people); }
    public void setFriendIds(Set<Long> ids) { peopleSearch.setFriendIds(ids); }
    public void setPendingFriendIds(Set<Long> ids) { peopleSearch.setPendingIds(ids); }
    public void setPeopleSearchListener(Consumer<String> listener) { peopleSearch.setSearchListener(listener); }
    public void setAddFriendListener(Consumer<SendRequest> listener) { peopleSearch.setAddFriendListener(listener); }
    public void markFriendRequestSent(long userId) { peopleSearch.markRequestSent(userId); }
    public void setGroupCandidates(List<User> users) { groupCreate.setAvailableMembers(users); }
    public void setGroupUsers(List<User> users) { groupManagement.setUsers(users); }
    public void setGroups(List<GroupDetails> groups) { groupManagement.setGroups(groups); }
    public void setGroupMembers(List<ConversationMember> members) { groupManagement.setMembers(members); }
    public void setCreateGroupListener(Consumer<GroupCreateRequest> listener) { groupCreate.setCreateListener(listener); }
    public void setGroupSelectedListener(Consumer<Long> listener) { groupManagement.setGroupSelectedListener(listener); }
    public void setUpdateGroupListener(Consumer<GroupUpdateRequest> listener) { groupManagement.setUpdateListener(listener); }
    public void setRemoveGroupMemberListener(Consumer<MemberRemoveRequest> listener) { groupManagement.setRemoveListener(listener); }
    public void setMemberRoleListener(Consumer<MemberRoleUpdateRequest> listener) { groupManagement.setRoleListener(listener); }

    private void showPeopleSearch() {
        showDetail(peopleSearch);
        peopleSearch.focusSearch();
    }

    private void showDetail(JPanel panel) {
        conversations.clearSelection();
        detail.removeAll();
        detail.add(panel, BorderLayout.CENTER);
        detail.revalidate();
        detail.repaint();
    }

    private void selectConversation(ConversationSummary item) {
        if (item == null) {
            showWelcome();
            return;
        }
        detail.removeAll();
        showChats();
        chat.setConversation(item);
        detail.add(chat, BorderLayout.CENTER);
        detail.revalidate();
        detail.repaint();
        openConversation.accept(item);
    }

    private void showWelcome() {
        detail.removeAll();
        detail.add(emptyDetail("Chào mừng đến Java Chat", "Chọn một hội thoại để bắt đầu trò chuyện."),
                BorderLayout.CENTER);
        detail.revalidate();
        detail.repaint();
    }

    private static JPanel emptyDetail(String title, String subtitle) {
        JPanel wrapper = new WelcomePanel();
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        BubbleIllustration illustration = new BubbleIllustration();
        illustration.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        content.add(illustration);
        content.add(Box.createVerticalStrut(10));
        JLabel heading = new JLabel(title, SwingConstants.CENTER);
        heading.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        heading.setForeground(INK);
        content.add(heading);
        content.add(Box.createVerticalStrut(14));
        JLabel description = new JLabel(subtitle, SwingConstants.CENTER);
        description.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        description.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        description.setForeground(MUTED);
        content.add(description);
        wrapper.add(content);
        return wrapper;
    }

    private static final class WelcomePanel extends JPanel {
        WelcomePanel() {
            super(new java.awt.GridBagLayout());
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(ChatTheme.MILK);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setPaint(new java.awt.RadialGradientPaint(
                    new java.awt.Point(getWidth(), getHeight()), Math.max(getWidth(), getHeight()) * .9f,
                    new float[] {0f, 1f}, new Color[] {new Color(199, 239, 222, 205), ChatTheme.MILK}));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(96, 195, 171, 30));
            g.fill(new Ellipse2D.Double(-180, -240, Math.max(420, getWidth() * .7), 420));
            g.setColor(new Color(224, 205, 170, 38));
            g.fill(new Ellipse2D.Double(getWidth() - 280, getHeight() - 180, 450, 340));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class BubbleIllustration extends JPanel {
        BubbleIllustration() {
            setOpaque(false);
            setPreferredSize(new Dimension(430, 235));
            setMaximumSize(getPreferredSize());
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate((getWidth() - 350) / 2, 0);
            g.setColor(new Color(215, 241, 229, 170));
            g.fillOval(70, 15, 215, 205);
            g.setColor(new Color(113, 199, 174, 52));
            g.fillOval(105, 209, 150, 14);
            g.setPaint(new java.awt.GradientPaint(160, 48, new Color(179, 233, 215),
                    280, 150, new Color(111, 196, 178)));
            g.fillRoundRect(171, 56, 118, 83, 25, 25);
            g.setPaint(new java.awt.GradientPaint(85, 80, new Color(88, 208, 180),
                    260, 200, new Color(24, 143, 138)));
            g.fillRoundRect(93, 96, 166, 95, 30, 30);
            g.fillPolygon(new int[] {118, 118, 106}, new int[] {181, 204, 190}, 3);
            g.setColor(new Color(255, 255, 255, 230));
            g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(126, 126, 224, 126);
            g.drawLine(126, 146, 205, 146);
            g.drawLine(126, 166, 179, 166);
            g.setColor(new Color(40, 166, 151, 150));
            sparkle(g, 82, 47, 14);
            sparkle(g, 276, 28, 10);
            g.setColor(new Color(96, 191, 166, 145));
            leaf(g, 55, 165, -35);
            leaf(g, 72, 182, 30);
            leaf(g, 288, 170, 35);
            leaf(g, 305, 184, -30);
            g.dispose();
        }

        private static void sparkle(Graphics2D g, int x, int y, int radius) {
            g.fillPolygon(new int[] {x, x + radius / 3, x + radius, x + radius / 3,
                    x, x - radius / 3, x - radius, x - radius / 3},
                    new int[] {y - radius, y - radius / 3, y, y + radius / 3,
                            y + radius, y + radius / 3, y, y - radius / 3}, 8);
        }

        private static void leaf(Graphics2D g, int x, int y, int angle) {
            Graphics2D leaf = (Graphics2D) g.create();
            leaf.rotate(Math.toRadians(angle), x, y);
            leaf.fillOval(x - 12, y - 25, 23, 42);
            leaf.dispose();
        }
    }
}
