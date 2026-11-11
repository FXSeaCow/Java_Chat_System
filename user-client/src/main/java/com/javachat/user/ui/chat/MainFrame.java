package com.javachat.user.ui.chat;

import com.javachat.shared.dto.ChatDtos.ConversationSummary;
import com.javachat.shared.dto.ChatDtos.MessageSendRequest;
import com.javachat.shared.model.ChatMessage;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
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
        add(conversations, BorderLayout.WEST);
        detail.setBackground(CANVAS);
        add(detail, BorderLayout.CENTER);
        showWelcome();
        conversations.setSelectionListener(this::selectConversation);
    }

    /** Supply results from CONVERSATION_LIST_RESPONSE on the Swing event dispatch thread. */
    public void setConversations(List<ConversationSummary> items) {
        conversations.setConversations(items);
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

    private void selectConversation(ConversationSummary item) {
        if (item == null) {
            showWelcome();
            return;
        }
        detail.removeAll();
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
