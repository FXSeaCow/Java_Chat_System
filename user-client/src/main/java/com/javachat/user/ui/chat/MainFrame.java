package com.javachat.user.ui.chat;

import com.javachat.shared.dto.ChatDtos.ConversationSummary;
import com.javachat.shared.dto.ChatDtos.MessageSendRequest;
import com.javachat.shared.dto.AiDtos;
import com.javachat.shared.model.ChatMessage;
import com.javachat.shared.dto.AuthDtos.PresenceChangedEvent;
import com.javachat.shared.dto.FriendDtos.FriendListResponse;
import com.javachat.shared.dto.FriendDtos.RespondRequest;
import com.javachat.shared.model.User;
import com.javachat.user.ui.friends.FriendListPanel;
import com.javachat.user.ui.friends.FriendRequestsPanel;
import com.javachat.user.ui.friends.OnlineFriendsPanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Consumer;

/** Main chat shell: conversation list on the left, chat panel on the right. */
public final class MainFrame extends JFrame {
    private static final Color INK = new Color(30, 48, 72);
    private static final Color MUTED = new Color(113, 132, 157);
    private static final Color BLUE = new Color(45, 112, 220);
    private static final Color CANVAS = new Color(238, 245, 252);

    private final ConversationListPanel conversations = new ConversationListPanel();
    private final FriendListPanel friends = new FriendListPanel();
    private final FriendRequestsPanel friendRequests = new FriendRequestsPanel();
    private final OnlineFriendsPanel onlineFriends = new OnlineFriendsPanel();
    private final CardLayout pages = new CardLayout();
    private final JPanel pageContainer = new JPanel(pages);
    private final JPanel detail = new JPanel(new BorderLayout());
    private final ChatPanel chat = new ChatPanel();
    private Consumer<ConversationSummary> openConversation = ignored -> { };

    public MainFrame() {
        super("Java Chat");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 720);
        setMinimumSize(new Dimension(830, 520));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);
        JPanel navigation = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 12, 8));
        navigation.setBackground(Color.WHITE);
        JButton chatsButton = new JButton("Tin nhắn");
        JButton friendsButton = new JButton("Bạn bè");
        JButton onlineButton = new JButton("Đang online");
        JButton requestsButton = new JButton("Lời mời kết bạn");
        chatsButton.addActionListener(event -> showChats());
        friendsButton.addActionListener(event -> showFriends());
        onlineButton.addActionListener(event -> showOnlineFriends());
        requestsButton.addActionListener(event -> showFriendRequests());
        navigation.add(chatsButton);
        navigation.add(friendsButton);
        navigation.add(onlineButton);
        navigation.add(requestsButton);
        add(navigation, BorderLayout.NORTH);
        JPanel chatPage = new JPanel(new BorderLayout());
        chatPage.add(conversations, BorderLayout.WEST);
        detail.setBackground(CANVAS);
        chatPage.add(detail, BorderLayout.CENTER);
        pageContainer.add(chatPage, "chats");
        pageContainer.add(friends, "friends");
        pageContainer.add(onlineFriends, "online");
        pageContainer.add(friendRequests, "requests");
        add(pageContainer, BorderLayout.CENTER);
        showWelcome();
        conversations.setSelectionListener(this::selectConversation);
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
    }

    public void showOnlineFriends() {
        pages.show(pageContainer, "online");
    }

    public void showFriendRequests() {
        pages.show(pageContainer, "requests");
    }

    public void showChats() {
        pages.show(pageContainer, "chats");
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
        JPanel wrapper = new JPanel(new java.awt.GridBagLayout());
        wrapper.setOpaque(false);
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        BubbleIllustration illustration = new BubbleIllustration();
        illustration.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        content.add(illustration);
        content.add(Box.createVerticalStrut(22));
        JLabel heading = new JLabel(title, SwingConstants.CENTER);
        heading.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        heading.setForeground(INK);
        content.add(heading);
        content.add(Box.createVerticalStrut(8));
        JLabel description = new JLabel(subtitle, SwingConstants.CENTER);
        description.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        description.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        description.setForeground(MUTED);
        content.add(description);
        wrapper.add(content);
        return wrapper;
    }

    private static final class BubbleIllustration extends JPanel {
        BubbleIllustration() {
            setOpaque(false);
            setPreferredSize(new Dimension(134, 110));
            setMaximumSize(getPreferredSize());
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(220, 235, 252));
            g.fillOval(15, 0, 104, 104);
            g.setColor(BLUE);
            g.fill(new RoundRectangle2D.Double(28, 26, 78, 54, 19, 19));
            g.fillPolygon(new int[] {42, 42, 32}, new int[] {76, 88, 77}, 3);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(45, 47, 88, 47);
            g.drawLine(45, 59, 76, 59);
            g.dispose();
        }
    }
}
