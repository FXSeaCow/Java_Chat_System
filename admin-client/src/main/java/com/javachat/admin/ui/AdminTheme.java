package com.javachat.admin.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

public final class AdminTheme {
    public static final Color BACKGROUND = new Color(246, 248, 252);
    public static final Color SURFACE = Color.WHITE;
    public static final Color SIDEBAR = new Color(15, 23, 42);
    public static final Color PRIMARY = new Color(37, 99, 235);
    public static final Color PRIMARY_SOFT = new Color(229, 239, 255);
    public static final Color TEXT = new Color(15, 23, 42);
    public static final Color MUTED = new Color(71, 85, 105);
    public static final Color BORDER = new Color(226, 232, 240);
    public static final Color SUCCESS = new Color(5, 150, 105);
    public static final Color WARNING = new Color(217, 119, 6);
    public static final Color DANGER = new Color(220, 38, 38);
    public static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    public static final Font MEDIUM = new Font(Font.SANS_SERIF, Font.BOLD, 14);

    private AdminTheme() { }

    public static JPanel page(String title, String subtitle, JComponent content) {
        JPanel page = new JPanel(new BorderLayout(0, 20));
        page.setBackground(BACKGROUND);
        page.setBorder(new EmptyBorder(28, 32, 28, 32));
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 4));
        heading.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        titleLabel.setForeground(TEXT);
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(BODY);
        subtitleLabel.setForeground(MUTED);
        heading.add(titleLabel);
        heading.add(subtitleLabel);
        page.add(heading, BorderLayout.NORTH);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    public static JPanel surface() {
        JPanel panel = new RoundedPanel(16);
        panel.setBackground(SURFACE);
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));
        return panel;
    }

    public static JTextField searchField(String accessibleName) {
        JTextField field = new JTextField();
        field.setFont(BODY);
        field.setForeground(TEXT);
        field.setBackground(SURFACE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(9, 12, 9, 12)));
        field.setPreferredSize(new Dimension(260, 38));
        field.getAccessibleContext().setAccessibleName(accessibleName);
        return field;
    }

    public static JButton button(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(MEDIUM);
        button.setForeground(primary ? Color.WHITE : PRIMARY);
        button.setBackground(primary ? PRIMARY : SURFACE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? PRIMARY : BORDER),
                new EmptyBorder(9, 14, 9, 14)));
        button.setFocusPainted(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JTable table(Object[][] rows, Object[] columns) {
        JTable table = new JTable(rows, columns);
        table.setFont(BODY);
        table.setForeground(TEXT);
        table.setRowHeight(42);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setSelectionBackground(PRIMARY_SOFT);
        table.setSelectionForeground(TEXT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setDefaultEditor(Object.class, null);
        JTableHeader header = table.getTableHeader();
        header.setFont(MEDIUM);
        header.setForeground(MUTED);
        header.setBackground(new Color(248, 250, 252));
        header.setPreferredSize(new Dimension(10, 40));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setBorder(new EmptyBorder(0, 10, 0, 10));
        table.setDefaultRenderer(Object.class, renderer);
        return table;
    }

    public static JScrollPane scroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(SURFACE);
        return scroll;
    }

    public static JLabel countBadge(String text, Color color) {
        JLabel badge = new JLabel(text);
        badge.setFont(MEDIUM);
        badge.setForeground(color);
        badge.setOpaque(true);
        badge.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 24));
        badge.setBorder(new EmptyBorder(6, 10, 6, 10));
        return badge;
    }

    private static final class RoundedPanel extends JPanel {
        private final int radius;
        RoundedPanel(int radius) { this.radius = radius; setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getBackground());
            g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g.setColor(BORDER);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
