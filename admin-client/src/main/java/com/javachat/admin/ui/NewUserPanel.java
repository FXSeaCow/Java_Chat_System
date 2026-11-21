package com.javachat.admin.ui;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;

final class NewUserPanel extends JPanel {
    NewUserPanel() {
        setLayout(new BorderLayout());
        JTable table = AdminTheme.table(new Object[][] {
                {"Mai Thanh Hà", "maiha", "maiha@example.com", "21/11/2026 09:27", "Đã xác minh"},
                {"Đỗ Quốc Bảo", "quocbao", "bao@example.com", "21/11/2026 08:43", "Chờ xác minh"},
                {"Võ Ngọc Lan", "ngoclan", "lan@example.com", "20/11/2026 22:19", "Đã xác minh"},
                {"Bùi Anh Tú", "anhtu", "tu@example.com", "20/11/2026 18:05", "Đã xác minh"},
                {"Phan Hoài An", "hoaian", "an@example.com", "20/11/2026 15:32", "Chờ xác minh"},
                {"Lâm Gia Hân", "giahan", "han@example.com", "19/11/2026 21:17", "Đã xác minh"}
        }, new Object[] {"Họ tên", "Tên đăng nhập", "Email", "Thời gian đăng ký", "Xác minh"});
        TableRowSorter<?> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        JPanel surface = AdminTheme.surface();
        surface.setLayout(new BorderLayout(0, 14));
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JTextField search = AdminTheme.searchField("Tìm người dùng mới");
        search.setToolTipText("Tìm theo họ tên, tên đăng nhập hoặc email");
        LoginHistoryPanel.bindSearch(search, sorter);
        toolbar.add(search, BorderLayout.WEST);
        JComboBox<String> period = new JComboBox<>(new String[] {"Hôm nay", "7 ngày", "30 ngày"});
        period.setFont(AdminTheme.BODY);
        period.getAccessibleContext().setAccessibleName("Khoảng thời gian đăng ký");
        toolbar.add(period, BorderLayout.EAST);
        surface.add(toolbar, BorderLayout.NORTH);
        surface.add(AdminTheme.scroll(table), BorderLayout.CENTER);
        surface.add(new JLabel("6 tài khoản mới · 2 tài khoản đang chờ xác minh email."), BorderLayout.SOUTH);
        add(AdminTheme.page("Người dùng đăng ký mới", "Theo dõi tài khoản mới và trạng thái xác minh ban đầu.", surface));
    }
}
