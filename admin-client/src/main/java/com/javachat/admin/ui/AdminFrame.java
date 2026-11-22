package com.javachat.admin.ui;

import com.javachat.admin.net.AdminApiClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** Admin shell aligned with the user client's cream header and teal rail. */
public final class AdminFrame extends JFrame {
    private static final int RAIL_COLLAPSED = 76;
    private static final int RAIL_EXPANDED = 270;
    private final CardLayout cardsLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardsLayout);
    private final Map<String, NavButton> navigation = new LinkedHashMap<>();
    private final AdminApiClient api;
    private Map<String, Object> snapshot;
    private String currentPage = "history";
    private JButton refreshButton;
    private RailPanel sidebar;
    private JLabel railBrand;
    private JLabel railFooter;
    private Timer railAnimation;
    private int railWidth = RAIL_COLLAPSED;
    private Point dragStart;

    public AdminFrame(AdminApiClient api, Map<String, Object> snapshot) {
        super("E-Chat Desktop - Quản trị");
        this.api = api;
        this.snapshot = snapshot;
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1_360, bounds.width - 40), Math.min(820, bounds.height - 40));
        setMinimumSize(new Dimension(1_050, 680));
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        getRootPane().registerKeyboardAction(event -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        showPage("history");
    }

    private JPanel buildContent() {
        cards.setBackground(AdminTheme.SURFACE);
        populateCards();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AdminTheme.BACKGROUND);
        root.add(buildHeader(), BorderLayout.NORTH);
        JPanel workspace = new JPanel(new BorderLayout(14, 0));
        workspace.setOpaque(false);
        workspace.setBorder(new EmptyBorder(0, 0, 12, 12));
        workspace.add(buildSidebar(), BorderLayout.WEST);
        JPanel pageSurface = new RoundedSurface();
        pageSurface.setLayout(new BorderLayout());
        pageSurface.setBorder(new EmptyBorder(8, 8, 8, 8));
        pageSurface.add(cards, BorderLayout.CENTER);
        workspace.add(pageSurface, BorderLayout.CENTER);
        root.add(workspace, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(14, 24, 16, 18));
        JComponent logo = new BrandMark();
        logo.setPreferredSize(new Dimension(52, 52));
        header.add(logo, BorderLayout.WEST);
        JPanel brand = new JPanel(new GridLayout(2, 1, 0, 2));
        brand.setOpaque(false);
        JLabel title = new JLabel("E-Chat Desktop");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        title.setForeground(AdminTheme.TEXT);
        JLabel caption = new JLabel("Quản trị hệ thống");
        caption.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        caption.setForeground(AdminTheme.MUTED);
        brand.add(title);
        brand.add(caption);
        header.add(brand, BorderLayout.CENTER);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        controls.setOpaque(false);
        refreshButton = AdminTheme.button("Làm mới", false);
        refreshButton.setToolTipText("Đọc lại dữ liệu từ Supabase");
        refreshButton.addActionListener(event -> refresh());
        JButton logout = AdminTheme.button("Đăng xuất", false);
        logout.addActionListener(event -> logout());
        JButton minimize = windowButton("−", "Thu nhỏ");
        JButton maximize = windowButton("□", "Phóng to hoặc khôi phục");
        JButton close = windowButton("×", "Đóng ứng dụng");
        minimize.addActionListener(event -> setState(JFrame.ICONIFIED));
        maximize.addActionListener(event -> setExtendedState(
                (getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0 ? JFrame.NORMAL : JFrame.MAXIMIZED_BOTH));
        close.addActionListener(event -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        controls.add(refreshButton);
        controls.add(logout);
        controls.add(minimize);
        controls.add(maximize);
        controls.add(close);
        header.add(controls, BorderLayout.EAST);
        MouseAdapter drag = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) { dragStart = event.getPoint(); }
            @Override public void mouseDragged(MouseEvent event) {
                if (dragStart == null || (getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0) return;
                Point point = event.getLocationOnScreen();
                setLocation(point.x - dragStart.x, point.y - dragStart.y);
            }
        };
        for (Component component : new Component[] {header, logo, brand, title, caption}) {
            component.addMouseListener(drag);
            component.addMouseMotionListener(drag);
        }
        return header;
    }

    private static JButton windowButton(String symbol, String tooltip) {
        JButton button = new WindowButton(symbol, "Đóng ứng dụng".equals(tooltip));
        button.setPreferredSize(new Dimension(38, 36));
        button.setToolTipText(tooltip);
        button.getAccessibleContext().setAccessibleName(tooltip);
        return button;
    }

    private JPanel buildSidebar() {
        sidebar = new RailPanel();
        sidebar.setLayout(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(RAIL_COLLAPSED, 700));
        sidebar.setBorder(new EmptyBorder(24, 10, 20, 10));
        railBrand = new JLabel("▣", SwingConstants.CENTER);
        railBrand.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        railBrand.setForeground(Color.WHITE);
        railBrand.setPreferredSize(new Dimension(50, 54));
        sidebar.add(railBrand, BorderLayout.NORTH);

        JPanel menu = new JPanel(new GridLayout(0, 1, 0, 8));
        menu.setOpaque(false);
        addNav(menu, "history", "◷", "Lịch sử đăng nhập");
        addNav(menu, "groups", "▤", "Danh sách nhóm chat");
        addNav(menu, "reports", "⚑", "Báo cáo spam");
        addNav(menu, "users", "◎", "Quản lý người dùng");
        addNav(menu, "new-users", "+", "Người dùng đăng ký mới");
        addNav(menu, "active-users", "◉", "Người dùng hoạt động");
        addNav(menu, "chart", "▥", "Biểu đồ đăng ký");
        JPanel menuHolder = new JPanel(new BorderLayout());
        menuHolder.setOpaque(false);
        menuHolder.setBorder(new EmptyBorder(34, 0, 0, 0));
        menuHolder.add(menu, BorderLayout.NORTH);
        sidebar.add(menuHolder, BorderLayout.CENTER);

        railFooter = new JLabel("◎", SwingConstants.CENTER);
        railFooter.setFont(AdminTheme.MEDIUM);
        railFooter.setForeground(new Color(222, 245, 241));
        railFooter.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(65, 119, 118)));
        railFooter.setPreferredSize(new Dimension(50, 54));
        sidebar.add(railFooter, BorderLayout.SOUTH);
        installRailHover(sidebar);
        return sidebar;
    }

    private void addNav(JPanel menu, String key, String icon, String label) {
        NavButton button = new NavButton(icon, label);
        button.setToolTipText(label);
        button.getAccessibleContext().setAccessibleName(label);
        button.addActionListener(event -> showPage(key));
        navigation.put(key, button);
        menu.add(button);
    }

    private void showPage(String key) {
        currentPage = key;
        cardsLayout.show(cards, key);
        navigation.forEach((name, button) -> button.setSelected(name.equals(key)));
    }

    private void populateCards() {
        cards.removeAll();
        cards.add(new LoginHistoryPanel(AdminData.rows(snapshot, "loginHistory")), "history");
        cards.add(new GroupListPanel(AdminData.rows(snapshot, "groups")), "groups");
        cards.add(new SpamReportPanel(AdminData.rows(snapshot, "reports"), this::updateReport), "reports");
        cards.add(new UserManagementPanel(AdminData.rows(snapshot, "users"), this::updateUserStatus), "users");
        cards.add(new NewUserPanel(AdminData.rows(snapshot, "newUsers")), "new-users");
        cards.add(new ActiveUserPanel(AdminData.rows(snapshot, "activeUsers")), "active-users");
        cards.add(new RegistrationChartPanel(AdminData.rows(snapshot, "registrationTrend")), "chart");
        cards.revalidate();
        cards.repaint();
    }

    private void refresh() {
        refreshButton.setEnabled(false);
        new SwingWorker<Map<String, Object>, Void>() {
            @Override protected Map<String, Object> doInBackground() throws Exception { return api.snapshot(); }
            @Override protected void done() {
                refreshButton.setEnabled(true);
                try {
                    snapshot = get();
                    populateCards();
                    showPage(currentPage);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException error) {
                    showError(error.getCause());
                }
            }
        }.execute();
    }

    private void updateReport(long reportId, String status) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                api.updateReport(reportId, status);
                return null;
            }
            @Override protected void done() {
                try { get(); refresh(); }
                catch (InterruptedException error) { Thread.currentThread().interrupt(); }
                catch (ExecutionException error) { showError(error.getCause()); }
            }
        }.execute();
    }

    private void updateUserStatus(long userId, String status) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                api.updateUserStatus(userId, status);
                return null;
            }
            @Override protected void done() {
                try { get(); refresh(); }
                catch (InterruptedException error) { Thread.currentThread().interrupt(); }
                catch (ExecutionException error) { showError(error.getCause()); }
            }
        }.execute();
    }

    private void logout() {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { api.logout(); return null; }
            @Override protected void done() {
                try { get(); }
                catch (InterruptedException error) { Thread.currentThread().interrupt(); }
                catch (ExecutionException error) { showError(error.getCause()); }
                dispose();
                new AdminLoginFrame().setVisible(true);
            }
        }.execute();
    }

    private void showError(Throwable error) {
        JOptionPane.showMessageDialog(this, error.getMessage(), "Lỗi API quản trị", JOptionPane.ERROR_MESSAGE);
    }

    private void installRailHover(Component component) {
        MouseAdapter listener = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent event) { animateRail(RAIL_EXPANDED); }
            @Override public void mouseExited(MouseEvent event) {
                Point point = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), sidebar);
                if (!sidebar.contains(point)) animateRail(RAIL_COLLAPSED);
            }
        };
        component.addMouseListener(listener);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) installRailHover(child);
        }
    }

    private void animateRail(int target) {
        if (railAnimation != null) railAnimation.stop();
        railAnimation = new Timer(12, event -> {
            int difference = target - railWidth;
            if (Math.abs(difference) <= 12) {
                railWidth = target;
                railAnimation.stop();
            } else {
                railWidth += difference > 0 ? 18 : -18;
            }
            sidebar.setPreferredSize(new Dimension(railWidth, 0));
            boolean expanded = railWidth >= 170;
            railBrand.setText(expanded ? "▣   E-Chat" : "▣");
            railBrand.setHorizontalAlignment(expanded ? SwingConstants.LEFT : SwingConstants.CENTER);
            railFooter.setText(expanded ? "◎   Quản trị viên" : "◎");
            railFooter.setHorizontalAlignment(expanded ? SwingConstants.LEFT : SwingConstants.CENTER);
            for (NavButton button : navigation.values()) button.setExpanded(expanded);
            sidebar.revalidate();
            sidebar.repaint();
        });
        railAnimation.start();
    }

    private static final class RailPanel extends JPanel {
        RailPanel() { setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, AdminTheme.SIDEBAR, 0, getHeight(), AdminTheme.PRIMARY_DARK));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class BrandMark extends JComponent {
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(AdminTheme.PRIMARY);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 13, 13);
            g.setColor(Color.WHITE);
            g.fillRoundRect(10, 13, 32, 24, 8, 8);
            g.fillPolygon(new int[] {15, 15, 21}, new int[] {34, 43, 35}, 3);
            g.setColor(AdminTheme.PRIMARY_DARK);
            for (int x : new int[] {20, 26, 32}) g.fillOval(x, 23, 3, 3);
            g.dispose();
        }
    }

    private static final class WindowButton extends JButton {
        private final boolean close;
        WindowButton(String symbol, boolean close) {
            super(symbol);
            this.close = close;
            setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 21));
            setForeground(AdminTheme.TEXT);
            setBorder(new EmptyBorder(0, 0, 0, 0));
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getModel().isRollover() || getModel().isPressed()) {
                g.setColor(close ? new Color(250, 222, 215) : AdminTheme.PRIMARY_SOFT);
                g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class RoundedSurface extends JPanel {
        RoundedSurface() { setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(AdminTheme.SURFACE);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class NavButton extends JButton {
        private final String icon;
        private final String label;
        NavButton(String icon, String label) {
            super(icon);
            this.icon = icon;
            this.label = label;
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(AdminTheme.MEDIUM);
            setForeground(Color.WHITE);
            setBorder(new EmptyBorder(13, 14, 13, 14));
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        void setExpanded(boolean expanded) {
            setText(expanded ? icon + "   " + label : icon);
            setHorizontalAlignment(expanded ? SwingConstants.LEFT : SwingConstants.CENTER);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isSelected() || getModel().isRollover()) {
                g.setColor(isSelected() ? AdminTheme.PRIMARY : new Color(255, 255, 255, 28));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
