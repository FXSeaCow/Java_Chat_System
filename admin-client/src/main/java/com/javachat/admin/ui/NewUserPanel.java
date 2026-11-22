package com.javachat.admin.ui;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Map;

final class NewUserPanel extends JPanel {
    NewUserPanel(List<Map<String, Object>> users) {
        setLayout(new BorderLayout());
        JTable table = AdminTheme.table(AdminData.table(users, row -> new Object[] {
                AdminData.text(row, "display_name"), AdminData.text(row, "username"),
                AdminData.text(row, "email"), AdminData.time(row, "created_at"),
                AdminData.accountStatus(row)
        }), new Object[] {"Họ tên", "Tên đăng nhập", "Email", "Thời gian đăng ký", "Trạng thái"});
        AdminTheme.columnWidths(table, 170, 160, 230, 185, 145);
        TableRowSorter<?> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        JPanel surface = AdminTheme.surface();
        surface.setLayout(new BorderLayout(0, 14));
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JTextField search = AdminTheme.searchField("Tìm người dùng mới");
        search.setToolTipText("Tìm theo họ tên, tên đăng nhập hoặc email");
        toolbar.add(search, BorderLayout.WEST);
        JComboBox<String> period = new JComboBox<>(new String[] {"Tất cả", "7 ngày", "30 ngày"});
        AdminTheme.styleComboBox(period);
        period.getAccessibleContext().setAccessibleName("Khoảng thời gian đăng ký");
        AdminData.bindDateFilter(search, period, sorter, users, "created_at");
        toolbar.add(period, BorderLayout.EAST);
        surface.add(toolbar, BorderLayout.NORTH);
        surface.add(AdminTheme.scroll(table), BorderLayout.CENTER);
        surface.add(new JLabel(users.size() + " tài khoản gần nhất · Dữ liệu Supabase."), BorderLayout.SOUTH);
        add(AdminTheme.page("Người dùng đăng ký mới", "Theo dõi tài khoản mới và trạng thái tài khoản.", surface));
    }
}
