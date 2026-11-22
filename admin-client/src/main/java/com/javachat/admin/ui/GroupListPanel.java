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
import java.util.List;
import java.util.Map;

final class GroupListPanel extends JPanel {
    GroupListPanel(List<Map<String, Object>> groups) {
        setLayout(new BorderLayout());
        JTable table = AdminTheme.table(AdminData.table(groups, row -> new Object[] {
                AdminData.text(row, "name"), AdminData.text(row, "owner_name"),
                AdminData.number(row, "member_count"), AdminData.number(row, "message_count"),
                AdminData.time(row, "updated_at"), AdminData.text(row, "description")
        }), new Object[] {"Tên nhóm", "Chủ nhóm", "Thành viên", "Tin nhắn", "Hoạt động cuối", "Mô tả"});
        AdminTheme.columnWidths(table, 250, 170, 100, 95, 175, 210);
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
        actions.add(AdminTheme.countBadge(groups.size() + " nhóm", AdminTheme.PRIMARY));
        JButton details = AdminTheme.button("Xem chi tiết", false);
        details.addActionListener(event -> showSelection(table));
        actions.add(details);
        toolbar.add(actions, BorderLayout.EAST);
        surface.add(toolbar, BorderLayout.NORTH);
        surface.add(AdminTheme.scroll(table), BorderLayout.CENTER);
        surface.add(new JLabel("Chọn một nhóm để xem thông tin quản trị chi tiết · Dữ liệu Supabase."), BorderLayout.SOUTH);
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
