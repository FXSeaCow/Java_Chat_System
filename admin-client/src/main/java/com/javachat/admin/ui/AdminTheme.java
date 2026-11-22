package com.javachat.admin.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;

/** Cream and teal colors shared with the user desktop client. */
public final class AdminTheme {
    public static final Color BACKGROUND = new Color(248, 244, 234);
    public static final Color SURFACE = new Color(255, 253, 248);
    public static final Color SIDEBAR = new Color(22, 65, 70);
    public static final Color PRIMARY = new Color(43, 174, 160);
    public static final Color PRIMARY_DARK = new Color(24, 115, 111);
    public static final Color PRIMARY_SOFT = new Color(225, 247, 240);
    public static final Color TEXT = new Color(25, 65, 70);
    public static final Color MUTED = new Color(111, 139, 138);
    public static final Color BORDER = new Color(229, 224, 211);
    public static final Color SUCCESS = PRIMARY_DARK;
    public static final Color WARNING = new Color(177, 112, 30);
    public static final Color DANGER = new Color(200, 70, 54);
    public static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    public static final Font MEDIUM = new Font(Font.SANS_SERIF, Font.BOLD, 14);

    private AdminTheme() { }

    public static JPanel page(String title, String subtitle, JComponent content) {
        JPanel page = new JPanel(new BorderLayout(0, 22));
        page.setBackground(SURFACE);
        page.setBorder(new EmptyBorder(32, 40, 32, 40));
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 4));
        heading.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
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
        JPanel panel = new RoundedPanel(18);
        panel.setBackground(SURFACE);
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));
        return panel;
    }

    public static JTextField searchField(String accessibleName) {
        JTextField field = new JTextField() {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                if (!getText().isEmpty() || hasFocus()) return;
                Graphics2D g = (Graphics2D) graphics.create();
                g.setColor(MUTED);
                g.setFont(getFont());
                g.drawString(accessibleName, 12, (getHeight() + g.getFontMetrics().getAscent()) / 2 - 2);
                g.dispose();
            }
        };
        field.setFont(BODY);
        field.setForeground(TEXT);
        field.setBackground(Color.WHITE);
        field.setCaretColor(PRIMARY_DARK);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(9, 12, 9, 12)));
        field.setPreferredSize(new Dimension(260, 40));
        field.getAccessibleContext().setAccessibleName(accessibleName);
        return field;
    }

    public static void styleComboBox(JComboBox<?> combo) {
        combo.setFont(BODY);
        combo.setForeground(TEXT);
        combo.setBackground(Color.WHITE);
        combo.setBorder(BorderFactory.createLineBorder(BORDER));
        combo.setPreferredSize(new Dimension(Math.max(150, combo.getPreferredSize().width + 12), 40));
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static JButton button(String text, boolean primary) {
        return new ActionButton(text, primary);
    }

    public static JTable table(Object[][] rows, Object[] columns) {
        JTable table = new JTable(rows, columns) {
            @Override public String getToolTipText(MouseEvent event) {
                int row = rowAtPoint(event.getPoint());
                int column = columnAtPoint(event.getPoint());
                if (row < 0 || column < 0) return null;
                Object value = getValueAt(row, column);
                return value == null ? null : String.valueOf(value);
            }
        };
        table.setFont(BODY);
        table.setForeground(TEXT);
        table.setBackground(SURFACE);
        table.setRowHeight(44);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setSelectionBackground(PRIMARY_SOFT);
        table.setSelectionForeground(TEXT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setDefaultEditor(Object.class, null);
        JTableHeader header = table.getTableHeader();
        header.setFont(MEDIUM);
        header.setForeground(TEXT);
        header.setBackground(BACKGROUND);
        header.setPreferredSize(new Dimension(10, 42));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setBorder(new EmptyBorder(0, 12, 0, 12));
        table.setDefaultRenderer(Object.class, renderer);
        DefaultTableCellRenderer numberRenderer = new DefaultTableCellRenderer();
        numberRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        numberRenderer.setBorder(new EmptyBorder(0, 12, 0, 12));
        table.setDefaultRenderer(Integer.class, numberRenderer);
        table.setDefaultRenderer(Long.class, numberRenderer);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    public static void columnWidths(JTable table, int... widths) {
        if (widths.length != table.getColumnCount()) {
            throw new IllegalArgumentException("Column width count must match table columns");
        }
        table.putClientProperty("admin.columnWidths", widths.clone());
        for (int index = 0; index < widths.length; index++) {
            TableColumn column = table.getColumnModel().getColumn(index);
            column.setMinWidth(widths[index]);
            column.setPreferredWidth(widths[index]);
            for (int row = 0; row < table.getRowCount(); row++) {
                if (table.getValueAt(row, index) instanceof Number) {
                    DefaultTableCellRenderer number = new DefaultTableCellRenderer();
                    number.setHorizontalAlignment(SwingConstants.RIGHT);
                    number.setBorder(new EmptyBorder(0, 12, 0, 12));
                    column.setCellRenderer(number);
                    break;
                }
            }
        }
    }

    public static JScrollPane scroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(SURFACE);
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) {
                int[] widths = (int[]) table.getClientProperty("admin.columnWidths");
                if (widths == null) return;
                int total = 0;
                for (int width : widths) total += width;
                int extra = Math.max(0, scroll.getViewport().getWidth() - total);
                int assigned = 0;
                for (int index = 0; index < widths.length; index++) {
                    int addition = index == widths.length - 1 ? extra - assigned : extra * widths[index] / total;
                    assigned += addition;
                    table.getColumnModel().getColumn(index).setPreferredWidth(widths[index] + addition);
                    table.getColumnModel().getColumn(index).setWidth(widths[index] + addition);
                }
                table.setSize(total + extra, table.getHeight());
                table.revalidate();
                table.repaint();
                table.getTableHeader().repaint();
            }
        });
        return scroll;
    }

    public static JLabel countBadge(String text, Color color) {
        JLabel badge = new JLabel(text);
        badge.setFont(MEDIUM);
        badge.setForeground(color);
        badge.setOpaque(true);
        badge.setBackground(new Color(
                (color.getRed() + 9 * SURFACE.getRed()) / 10,
                (color.getGreen() + 9 * SURFACE.getGreen()) / 10,
                (color.getBlue() + 9 * SURFACE.getBlue()) / 10));
        badge.setBorder(new EmptyBorder(7, 12, 7, 12));
        return badge;
    }

    private static final class ActionButton extends JButton {
        private final boolean primary;
        ActionButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(MEDIUM);
            setForeground(primary ? Color.WHITE : TEXT);
            setBorder(new EmptyBorder(10, 18, 10, 18));
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = primary ? PRIMARY : Color.WHITE;
            if (getModel().isPressed()) fill = primary ? PRIMARY_DARK : PRIMARY_SOFT;
            else if (getModel().isRollover()) fill = primary ? new Color(37, 153, 143) : PRIMARY_SOFT;
            if (!isEnabled()) fill = BACKGROUND;
            g.setColor(fill);
            g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 12, 12);
            g.setColor(primary ? fill : BORDER);
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
            if (hasFocus()) {
                g.setColor(PRIMARY_DARK);
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(3, 3, getWidth() - 7, getHeight() - 7, 10, 10);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
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
