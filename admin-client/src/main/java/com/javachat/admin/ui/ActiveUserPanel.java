package com.javachat.admin.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Map;

final class ActiveUserPanel extends JPanel {
    ActiveUserPanel(List<Map<String, Object>> users) {
        setLayout(new BorderLayout());
        JTable table = AdminTheme.table(AdminData.table(users, row -> new Object[] {
                AdminData.text(row, "display_name"), AdminData.text(row, "username"),
                "Đang đăng nhập", AdminData.time(row, "last_login"),
                AdminData.number(row, "messages_today"), AdminData.number(row, "session_count")
        }), new Object[] {"Người dùng", "Tên đăng nhập", "Phiên", "Đăng nhập gần nhất", "Tin nhắn hôm nay", "Số phiên"});
        AdminTheme.columnWidths(table, 180, 155, 135, 155, 160, 80);
        TableRowSorter<?> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        JPanel surface = AdminTheme.surface();
        surface.setLayout(new BorderLayout(0, 14));
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JTextField search = AdminTheme.searchField("Tìm người dùng đang hoạt động");
        search.setToolTipText("Tìm người dùng đang hoạt động");
        LoginHistoryPanel.bindSearch(search, sorter);
        toolbar.add(search, BorderLayout.WEST);
        toolbar.add(AdminTheme.countBadge("Phiên còn hiệu lực", AdminTheme.SUCCESS), BorderLayout.EAST);
        surface.add(toolbar, BorderLayout.NORTH);
        surface.add(AdminTheme.scroll(table), BorderLayout.CENTER);
        surface.add(new JLabel(users.size() + " người dùng có phiên đăng nhập còn hiệu lực · Dữ liệu Supabase."), BorderLayout.SOUTH);
        add(AdminTheme.page("Người dùng hoạt động", "Tài khoản có phiên đăng nhập còn hiệu lực; làm mới để cập nhật.", surface));
    }
}
