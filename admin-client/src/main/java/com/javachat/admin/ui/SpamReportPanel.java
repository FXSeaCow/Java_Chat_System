package com.javachat.admin.ui;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

final class SpamReportPanel extends JPanel {
    SpamReportPanel() {
        setLayout(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel(new Object[][] {
                {"#1042", "Nguyễn Minh Anh", "Tin nhắn quảng cáo lặp lại", "Người dùng #318", "21/11/2026 09:11", "Chờ xử lý"},
                {"#1041", "Trần Gia Huy", "Gửi liên kết đáng ngờ", "Người dùng #127", "21/11/2026 08:35", "Chờ xử lý"},
                {"#1038", "Lê Hoàng Nam", "Nội dung không liên quan", "Người dùng #204", "20/11/2026 19:20", "Đã xem"},
                {"#1035", "Nguyễn Thu Thảo", "Spam trong nhóm", "Người dùng #411", "20/11/2026 14:05", "Bỏ qua"}
        }, new Object[] {"Mã", "Người báo cáo", "Lý do", "Đối tượng", "Thời gian", "Trạng thái"}) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = AdminTheme.table(new Object[0][0], new Object[0]);
        table.setModel(model);

        JPanel surface = AdminTheme.surface();
        surface.setLayout(new BorderLayout(0, 14));
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JComboBox<String> filter = new JComboBox<>(new String[] {"Tất cả báo cáo", "Chờ xử lý", "Đã xem", "Bỏ qua"});
        filter.setFont(AdminTheme.BODY);
        filter.getAccessibleContext().setAccessibleName("Lọc trạng thái báo cáo");
        toolbar.add(filter, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton dismiss = AdminTheme.button("Bỏ qua", false);
        JButton review = AdminTheme.button("Đánh dấu đã xem", true);
        dismiss.addActionListener(event -> updateStatus(table, model, "Bỏ qua"));
        review.addActionListener(event -> updateStatus(table, model, "Đã xem"));
        actions.add(dismiss);
        actions.add(review);
        toolbar.add(actions, BorderLayout.EAST);
        surface.add(toolbar, BorderLayout.NORTH);
        surface.add(AdminTheme.scroll(table), BorderLayout.CENTER);
        surface.add(new JLabel("2 báo cáo đang chờ xử lý · Hành động quản trị cần chọn một dòng."), BorderLayout.SOUTH);
        add(AdminTheme.page("Báo cáo spam", "Xem xét báo cáo và ghi nhận quyết định xử lý minh bạch.", surface));
    }

    private void updateStatus(JTable table, DefaultTableModel model, String status) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một báo cáo trước khi xử lý.", "Chưa chọn báo cáo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        model.setValueAt(status, modelRow, 5);
    }
}
