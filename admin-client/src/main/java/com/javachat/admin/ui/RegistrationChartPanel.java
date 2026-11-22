package com.javachat.admin.ui;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

final class RegistrationChartPanel extends JPanel {
    RegistrationChartPanel(List<Map<String, Object>> trend) {
        setLayout(new BorderLayout());
        int[] registrations = trend.stream().mapToInt(row -> (int) AdminData.number(row, "registrations")).toArray();
        int[] logins = trend.stream().mapToInt(row -> (int) AdminData.number(row, "logins")).toArray();
        String[] labels = trend.stream().map(AdminData::day).toArray(String[]::new);

        JPanel surface = AdminTheme.surface();
        surface.setLayout(new BorderLayout(0, 16));
        JPanel summary = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        summary.setOpaque(false);
        JLabel registrationBadge = AdminTheme.countBadge("0 đăng ký", AdminTheme.PRIMARY);
        JLabel loginBadge = AdminTheme.countBadge("0 lượt đăng nhập", AdminTheme.SUCCESS);
        summary.add(registrationBadge);
        summary.add(loginBadge);
        JComboBox<String> period = new JComboBox<>(new String[] {"14 ngày", "30 ngày", "90 ngày"});
        AdminTheme.styleComboBox(period);
        period.getAccessibleContext().setAccessibleName("Khoảng thời gian biểu đồ");
        summary.add(period);
        surface.add(summary, BorderLayout.NORTH);

        JPanel visualization = new JPanel(new BorderLayout(0, 12));
        visualization.setOpaque(false);
        JPanel charts = new JPanel(new GridLayout(1, 2, 16, 0));
        charts.setOpaque(false);
        visualization.add(charts, BorderLayout.CENTER);
        JTable data = AdminTheme.table(new Object[0][0], new Object[0]);
        javax.swing.table.DefaultTableModel dataModel = new javax.swing.table.DefaultTableModel(
                new Object[][] {{"—", 0, 0}},
                new Object[] {"Giai đoạn", "Đăng ký mới", "Lượt đăng nhập"}) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        data.setModel(dataModel);
        AdminTheme.columnWidths(data, 190, 175, 195);
        data.setPreferredScrollableViewportSize(new Dimension(700, 88));
        visualization.add(AdminTheme.scroll(data), BorderLayout.SOUTH);
        surface.add(visualization, BorderLayout.CENTER);
        JLabel note = new JLabel("Dữ liệu Supabase · Thống kê theo ngày.");
        surface.add(note, BorderLayout.SOUTH);
        Runnable refresh = () -> {
            int days = switch (period.getSelectedIndex()) { case 1 -> 30; case 2 -> 90; default -> 14; };
            int start = Math.max(0, labels.length - days);
            String[] shownLabels = Arrays.copyOfRange(labels, start, labels.length);
            int[] shownRegistrations = Arrays.copyOfRange(registrations, start, registrations.length);
            int[] shownLogins = Arrays.copyOfRange(logins, start, logins.length);
            int registrationTotal = Arrays.stream(shownRegistrations).sum();
            int loginTotal = Arrays.stream(shownLogins).sum();
            registrationBadge.setText(registrationTotal + " đăng ký");
            loginBadge.setText(loginTotal + " lượt đăng nhập");
            charts.removeAll();
            charts.add(new LineChart("Đăng ký mới", shownLabels, shownRegistrations,
                    Math.max(4, Arrays.stream(shownRegistrations).max().orElse(0)), AdminTheme.PRIMARY,
                    "Số tài khoản mới theo ngày."));
            charts.add(new LineChart("Lượt đăng nhập", shownLabels, shownLogins,
                    Math.max(4, Arrays.stream(shownLogins).max().orElse(0)), AdminTheme.SUCCESS,
                    "Số tài khoản đăng nhập theo ngày."));
            dataModel.setDataVector(new Object[][] {
                    {shownLabels.length == 0 ? "—" : shownLabels[0] + "–" + shownLabels[shownLabels.length - 1],
                            registrationTotal, loginTotal}
            }, new Object[] {"Giai đoạn", "Đăng ký mới", "Lượt đăng nhập"});
            AdminTheme.columnWidths(data, 190, 175, 195);
            charts.revalidate();
            charts.repaint();
        };
        period.addActionListener(event -> refresh.run());
        refresh.run();
        add(AdminTheme.page("Đăng ký mới và đăng nhập", "So sánh số tài khoản mới và lượt đăng nhập theo ngày.", surface));
    }

    private static final class LineChart extends JPanel {
        private final String title;
        private final String[] labels;
        private final int[] values;
        private final int max;
        private final Color lineColor;

        LineChart(String title, String[] labels, int[] values, int max, Color lineColor, String description) {
            this.title = title;
            this.labels = Arrays.copyOf(labels, labels.length);
            this.values = Arrays.copyOf(values, values.length);
            this.max = max;
            this.lineColor = lineColor;
            setOpaque(false);
            setPreferredSize(new Dimension(700, 330));
            setToolTipText(title + " trong 14 ngày");
            getAccessibleContext().setAccessibleName("Biểu đồ " + title.toLowerCase());
            getAccessibleContext().setAccessibleDescription(description);
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int left = 48, right = 24, top = 28, bottom = 40;
            int width = Math.max(1, getWidth() - left - right);
            int height = Math.max(1, getHeight() - top - bottom);
            FontMetrics metrics = g.getFontMetrics(AdminTheme.BODY);
            g.setFont(AdminTheme.BODY);
            g.setColor(AdminTheme.TEXT);
            g.drawString(title, left, 16);
            for (int index = 0; index <= 4; index++) {
                int tick = index * max / 4;
                int y = top + height - index * height / 4;
                g.setColor(AdminTheme.BORDER);
                g.drawLine(left, y, left + width, y);
                g.setColor(AdminTheme.MUTED);
                g.drawString(String.valueOf(tick), left - metrics.stringWidth(String.valueOf(tick)) - 10, y + 5);
            }

            Path2D line = new Path2D.Double();
            for (int i = 0; i < values.length; i++) {
                double x = left + i * (width / (double) Math.max(1, values.length - 1));
                double y = top + height - values[i] * (height / (double) max);
                if (i == 0) line.moveTo(x, y); else line.lineTo(x, y);
            }
            g.setColor(lineColor);
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(line);
            for (int i = 0; i < values.length; i++) {
                int x = left + (int) Math.round(i * (width / (double) Math.max(1, values.length - 1)));
                int y = top + height - values[i] * height / max;
                g.setColor(Color.WHITE);
                g.fillOval(x - 5, y - 5, 10, 10);
                g.setColor(lineColor);
                g.setStroke(new BasicStroke(2));
                g.drawOval(x - 5, y - 5, 10, 10);
                if (i % Math.max(1, values.length / 7) == 0 || i == values.length - 1) {
                    String label = labels[i];
                    g.setColor(AdminTheme.MUTED);
                    g.drawString(label, x - metrics.stringWidth(label) / 2, top + height + 25);
                }
            }
            g.dispose();
        }
    }
}
