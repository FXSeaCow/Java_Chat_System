package com.javachat.admin.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AdminFrame extends JFrame {
    private final CardLayout cardsLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardsLayout);
    private final Map<String, JButton> navigation = new LinkedHashMap<>();

    public AdminFrame() {
        super("Java Chat - Quản trị hệ thống");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1_360, bounds.width - 60), Math.min(820, bounds.height - 60));
        setMinimumSize(new Dimension(1_050, 680));
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        getRootPane().registerKeyboardAction(event -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JPanel.WHEN_IN_FOCUSED_WINDOW);
        showPage("history");
    }

    private JPanel buildContent() {
        cards.setBackground(AdminTheme.BACKGROUND);
        cards.add(new LoginHistoryPanel(), "history");
        cards.add(new GroupListPanel(), "groups");
        cards.add(new SpamReportPanel(), "reports");
        cards.add(new NewUserPanel(), "new-users");
        cards.add(new ActiveUserPanel(), "active-users");
        cards.add(new RegistrationChartPanel(), "chart");

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(cards, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(AdminTheme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(250, 700));
        sidebar.setBorder(new EmptyBorder(24, 16, 20, 16));

        JPanel brand = new JPanel(new BorderLayout(12, 0));
        brand.setOpaque(false);
        JLabel mark = new JLabel("JC", SwingConstants.CENTER);
        mark.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        mark.setForeground(Color.WHITE);
        mark.setOpaque(true);
        mark.setBackground(AdminTheme.PRIMARY);
        mark.setPreferredSize(new Dimension(42, 42));
        JLabel name = new JLabel("<html><b>Java Chat</b><br><font color='#94A3B8'>Admin Console</font></html>");
        name.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        name.setForeground(Color.WHITE);
        brand.add(mark, BorderLayout.WEST);
        brand.add(name, BorderLayout.CENTER);
        sidebar.add(brand, BorderLayout.NORTH);

        JPanel menu = new JPanel(new GridLayout(0, 1, 0, 7));
        menu.setOpaque(false);
        addNav(menu, "history", "Lịch sử đăng nhập");
        addNav(menu, "groups", "Danh sách nhóm chat");
        addNav(menu, "reports", "Báo cáo spam");
        addNav(menu, "new-users", "Người dùng đăng ký mới");
        addNav(menu, "active-users", "Người dùng hoạt động");
        addNav(menu, "chart", "Biểu đồ đăng ký");
        JPanel menuHolder = new JPanel(new BorderLayout());
        menuHolder.setOpaque(false);
        menuHolder.setBorder(new EmptyBorder(28, 0, 0, 0));
        menuHolder.add(menu, BorderLayout.NORTH);
        sidebar.add(menuHolder, BorderLayout.CENTER);

        JLabel admin = new JLabel("  Quản trị viên");
        admin.setFont(AdminTheme.MEDIUM);
        admin.setForeground(new Color(203, 213, 225));
        admin.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(51, 65, 85)));
        admin.setPreferredSize(new Dimension(200, 48));
        sidebar.add(admin, BorderLayout.SOUTH);
        return sidebar;
    }

    private void addNav(JPanel menu, String key, String label) {
        JButton button = new JButton(label);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(AdminTheme.MEDIUM);
        button.setForeground(new Color(203, 213, 225));
        button.setBackground(AdminTheme.SIDEBAR);
        button.setBorder(new EmptyBorder(12, 14, 12, 14));
        button.setFocusPainted(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(event -> showPage(key));
        button.getAccessibleContext().setAccessibleName(label);
        navigation.put(key, button);
        menu.add(button);
    }

    private void showPage(String key) {
        cardsLayout.show(cards, key);
        navigation.forEach((name, button) -> {
            boolean active = name.equals(key);
            button.setBackground(active ? new Color(30, 64, 175) : AdminTheme.SIDEBAR);
            button.setForeground(active ? Color.WHITE : new Color(203, 213, 225));
        });
    }
}
