package com.javachat.admin.ui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

final class GroupListPanel extends JPanel {
    GroupListPanel() {
        setLayout(new BorderLayout());
        JTable table = AdminTheme.table(new Object[][] {
                {"Nhóm Java Chat", "Nguyễn Minh Anh", 18, 326, "21/11/2026 09:35", "Hoạt động"},
                {"Đồ án Công nghệ phần mềm", "Trần Gia Huy", 9, 184, "21/11/2026 08:12", "Hoạt động"},
                {"Cộng đồng Swing Việt Nam", "Lê Hoàng Nam", 42, 905, "20/11/2026 23:01", "Hoạt động"},
                {"Team Backend", "Nguyễn Thu Thảo", 6, 76, "19/11/2026 17:45", "Ít hoạt động"},
                {"Test Group 01", "Phạm Đức Long", 2, 3, "12/11/2026 10:20", "Ít hoạt động"}
        }, new Object[] {"Tên nhóm", "Chủ nhóm", "Thành viên", "Tin nhắn", "Hoạt động cuối", "Trạng thái"});
        TableRowSorter<?> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        JPanel surface = AdminTheme.surface();
        surface.setLayout(new BorderLayout(0, 14));
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JTextField search = AdminTheme.searchField("Tìm theo tên nhóm hoặc chủ nhóm");
        search.setToolTipText("Tìm theo tên nhóm hoặc chủ nhóm");
        LoginHistoryPanel.bindSearch(search, sorter);
        toolbar.add(search, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(AdminTheme.countBadge("5 nhóm", AdminTheme.PRIMARY));
        JButton details = AdminTheme.button("Xem chi tiết", false);
        details.addActionListener(event -> showSelection(table));
        actions.add(details);
        toolbar.add(actions, BorderLayout.EAST);
        surface.add(toolbar, BorderLayout.NORTH);
        surface.add(AdminTheme.scroll(table), BorderLayout.CENTER);
        surface.add(new JLabel("Chọn một nhóm để xem thông tin quản trị chi tiết."), BorderLayout.SOUTH);
        add(AdminTheme.page("Danh sách nhóm chat", "Kiểm tra quy mô, chủ sở hữu và mức độ hoạt động của nhóm.", surface));
    }

    private void showSelection(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một nhóm trong danh sách.", "Chưa chọn nhóm", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this,
                "Nhóm: " + table.getValueAt(row, 0) + "\nChủ nhóm: " + table.getValueAt(row, 1)
                        + "\nThành viên: " + table.getValueAt(row, 2),
                "Chi tiết nhóm", JOptionPane.INFORMATION_MESSAGE);
    }
}
