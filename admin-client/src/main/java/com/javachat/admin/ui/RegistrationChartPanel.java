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

final class RegistrationChartPanel extends JPanel {
    RegistrationChartPanel() {
        setLayout(new BorderLayout());
        int[] registrations = {12, 18, 15, 24, 31, 28, 42, 38, 46, 52, 49, 61, 58, 72};
        int[] activeUsers = {184, 205, 198, 232, 276, 261, 310, 294, 338, 365, 352, 401, 389, 438};
        String[] labels = {"08/11", "09/11", "10/11", "11/11", "12/11", "13/11", "14/11",
                "15/11", "16/11", "17/11", "18/11", "19/11", "20/11", "21/11"};

        JPanel surface = AdminTheme.surface();
        surface.setLayout(new BorderLayout(0, 16));
        JPanel summary = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        summary.setOpaque(false);
        summary.add(AdminTheme.countBadge("546 đăng ký", AdminTheme.PRIMARY));
        summary.add(AdminTheme.countBadge("438 người hoạt động", AdminTheme.SUCCESS));
        summary.add(AdminTheme.countBadge("+18,4% so với kỳ trước", AdminTheme.SUCCESS));
        JComboBox<String> period = new JComboBox<>(new String[] {"14 ngày", "30 ngày", "90 ngày"});
        period.setFont(AdminTheme.BODY);
        period.getAccessibleContext().setAccessibleName("Khoảng thời gian biểu đồ");
        summary.add(period);
        surface.add(summary, BorderLayout.NORTH);

        JPanel visualization = new JPanel(new BorderLayout(0, 12));
        visualization.setOpaque(false);
        JPanel charts = new JPanel(new GridLayout(1, 2, 16, 0));
        charts.setOpaque(false);
        charts.add(new LineChart("Đăng ký mới", labels, registrations, 80, AdminTheme.PRIMARY,
                "Tăng từ 12 lượt ngày 08 tháng 11 lên 72 lượt ngày 21 tháng 11."));
        charts.add(new LineChart("Người dùng hoạt động", labels, activeUsers, 500, AdminTheme.SUCCESS,
                "Tăng từ 184 người ngày 08 tháng 11 lên 438 người ngày 21 tháng 11."));
        visualization.add(charts, BorderLayout.CENTER);
        JTable data = AdminTheme.table(new Object[][] {
                {"08–14/11", 140, 310, "—"},
                {"15–21/11", 376, 438, "+18,4% hoạt động"}
        }, new Object[] {"Giai đoạn", "Đăng ký mới", "Hoạt động cuối kỳ", "Thay đổi"});
        data.setPreferredScrollableViewportSize(new Dimension(700, 88));
        visualization.add(AdminTheme.scroll(data), BorderLayout.SOUTH);
        surface.add(visualization, BorderLayout.CENTER);
        surface.add(new JLabel("Ngày 21/11 đạt 72 đăng ký mới và 438 người dùng hoạt động, cao nhất kỳ."), BorderLayout.SOUTH);
        add(AdminTheme.page("Đăng ký mới và hoạt động", "So sánh hai xu hướng bằng biểu đồ tách thang đo và bảng dữ liệu.", surface));
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
            int step = max / 4;
            for (int tick = 0; tick <= max; tick += step) {
                int y = top + height - tick * height / max;
                g.setColor(AdminTheme.BORDER);
                g.drawLine(left, y, left + width, y);
                g.setColor(AdminTheme.MUTED);
                g.drawString(String.valueOf(tick), left - metrics.stringWidth(String.valueOf(tick)) - 10, y + 5);
            }

            Path2D line = new Path2D.Double();
            for (int i = 0; i < values.length; i++) {
                double x = left + i * (width / (double) (values.length - 1));
                double y = top + height - values[i] * (height / (double) max);
                if (i == 0) line.moveTo(x, y); else line.lineTo(x, y);
            }
            g.setColor(lineColor);
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(line);
            for (int i = 0; i < values.length; i++) {
                int x = left + (int) Math.round(i * (width / (double) (values.length - 1)));
                int y = top + height - values[i] * height / max;
                g.setColor(Color.WHITE);
                g.fillOval(x - 5, y - 5, 10, 10);
                g.setColor(lineColor);
                g.setStroke(new BasicStroke(2));
                g.drawOval(x - 5, y - 5, 10, 10);
                if (i % 2 == 0 || i == values.length - 1) {
                    String label = labels[i];
                    g.setColor(AdminTheme.MUTED);
                    g.drawString(label, x - metrics.stringWidth(label) / 2, top + height + 25);
                }
            }
            g.dispose();
        }
    }
}
