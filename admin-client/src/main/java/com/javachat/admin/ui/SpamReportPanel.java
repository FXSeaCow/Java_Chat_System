package com.javachat.admin.ui;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

final class SpamReportPanel extends JPanel {
    SpamReportPanel(List<Map<String, Object>> reports, BiConsumer<Long, String> onStatusChange) {
        setLayout(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel(AdminData.table(reports, row -> new Object[] {
                "#" + AdminData.number(row, "id"), AdminData.text(row, "reporter_name"),
                AdminData.text(row, "reason"), AdminData.text(row, "target_name"),
                AdminData.time(row, "created_at"), statusLabel(AdminData.text(row, "status"))
        }), new Object[] {"Mã", "Người báo cáo", "Lý do", "Đối tượng", "Thời gian", "Trạng thái"}) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = AdminTheme.table(new Object[0][0], new Object[0]);
        table.setModel(model);
        AdminTheme.columnWidths(table, 80, 170, 220, 165, 175, 120);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JPanel surface = AdminTheme.surface();
        surface.setLayout(new BorderLayout(0, 14));
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JComboBox<String> filter = new JComboBox<>(new String[] {"Tất cả báo cáo", "Chờ xử lý", "Đã xem", "Bỏ qua"});
        AdminTheme.styleComboBox(filter);
        filter.getAccessibleContext().setAccessibleName("Lọc trạng thái báo cáo");
        filter.addActionListener(event -> {
            int selected = filter.getSelectedIndex();
            sorter.setRowFilter(selected == 0 ? null : javax.swing.RowFilter.regexFilter(
                    "^" + java.util.regex.Pattern.quote(filter.getSelectedItem().toString()) + "$", 5));
        });
        toolbar.add(filter, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton dismiss = AdminTheme.button("Bỏ qua", false);
        JButton review = AdminTheme.button("Đánh dấu đã xem", true);
        dismiss.addActionListener(event -> updateStatus(table, reports, onStatusChange, "DISMISSED"));
        review.addActionListener(event -> updateStatus(table, reports, onStatusChange, "REVIEWED"));
        actions.add(dismiss);
        actions.add(review);
        toolbar.add(actions, BorderLayout.EAST);
        surface.add(toolbar, BorderLayout.NORTH);
        surface.add(AdminTheme.scroll(table), BorderLayout.CENTER);
        long pending = reports.stream().filter(row -> "PENDING".equals(AdminData.text(row, "status"))).count();
        surface.add(new JLabel(pending + " báo cáo đang chờ xử lý · Chọn một dòng để cập nhật Supabase."), BorderLayout.SOUTH);
        add(AdminTheme.page("Báo cáo spam", "Xem xét báo cáo và ghi nhận quyết định xử lý minh bạch.", surface));
    }

    private static String statusLabel(String status) {
        return switch (status) {
            case "PENDING" -> "Chờ xử lý";
            case "REVIEWED" -> "Đã xem";
            case "DISMISSED" -> "Bỏ qua";
            default -> status;
        };
    }

    private void updateStatus(JTable table, List<Map<String, Object>> reports,
                              BiConsumer<Long, String> onStatusChange, String status) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một báo cáo trước khi xử lý.", "Chưa chọn báo cáo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        if (!"PENDING".equals(AdminData.text(reports.get(modelRow), "status"))) {
            JOptionPane.showMessageDialog(this, "Báo cáo này đã được xử lý.");
            return;
        }
        onStatusChange.accept(AdminData.number(reports.get(modelRow), "id"), status);
    }
}
