package com.javachat.admin.ui;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;

final class ActiveUserPanel extends JPanel {
    ActiveUserPanel() {
        setLayout(new BorderLayout());
        JTable table = AdminTheme.table(new Object[][] {
                {"Nguyễn Minh Anh", "minhanh", "Trực tuyến", "2 phút trước", 32, 4},
                {"Trần Gia Huy", "giahuy", "Trực tuyến", "5 phút trước", 18, 2},
                {"Lê Hoàng Nam", "hoangnam", "Bận", "8 phút trước", 27, 5},
                {"Nguyễn Thu Thảo", "thao.nguyen", "Trực tuyến", "12 phút trước", 14, 3},
                {"Phạm Đức Long", "duclong", "Vắng mặt", "28 phút trước", 8, 1},
                {"Đỗ Quốc Bảo", "quocbao", "Trực tuyến", "31 phút trước", 6, 2}
        }, new Object[] {"Người dùng", "Tên đăng nhập", "Hiện diện", "Hoạt động cuối", "Tin nhắn hôm nay", "Phiên"});
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
        JComboBox<String> presence = new JComboBox<>(new String[] {"Tất cả trạng thái", "Trực tuyến", "Bận", "Vắng mặt"});
        presence.setFont(AdminTheme.BODY);
        presence.getAccessibleContext().setAccessibleName("Lọc trạng thái hiện diện");
        toolbar.add(presence, BorderLayout.EAST);
        surface.add(toolbar, BorderLayout.NORTH);
        surface.add(AdminTheme.scroll(table), BorderLayout.CENTER);
        surface.add(new JLabel("6 người dùng hoạt động trong 30 phút gần nhất."), BorderLayout.SOUTH);
        add(AdminTheme.page("Người dùng hoạt động", "Quan sát hiện diện và mức độ sử dụng gần đây theo thời gian thực.", surface));
    }
}
