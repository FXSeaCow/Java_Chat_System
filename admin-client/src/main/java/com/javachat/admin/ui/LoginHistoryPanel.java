package com.javachat.admin.ui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Map;

final class LoginHistoryPanel extends JPanel {
    LoginHistoryPanel(List<Map<String, Object>> sessions) {
        setLayout(new BorderLayout());
        JTable table = AdminTheme.table(AdminData.table(sessions, row -> new Object[] {
                AdminData.text(row, "display_name"), AdminData.text(row, "email"),
                AdminData.text(row, "device_info"), AdminData.text(row, "ip_address"),
                AdminData.time(row, "logged_in_at"), AdminData.text(row, "session_status")
        }), new Object[] {"Người dùng", "Email", "Thiết bị", "Địa chỉ IP", "Đăng nhập lúc", "Trạng thái"});
        AdminTheme.columnWidths(table, 145, 185, 175, 130, 165, 140);
        TableRowSorter<?> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        JPanel surface = AdminTheme.surface();
        surface.setLayout(new BorderLayout(0, 14));
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JTextField search = AdminTheme.searchField("Tìm người dùng, email hoặc địa chỉ IP");
        search.setToolTipText("Tìm người dùng, email hoặc địa chỉ IP");
        toolbar.add(search, BorderLayout.WEST);
        JComboBox<String> period = new JComboBox<>(new String[] {"Tất cả", "7 ngày gần đây", "30 ngày gần đây"});
        AdminTheme.styleComboBox(period);
        period.getAccessibleContext().setAccessibleName("Khoảng thời gian");
        AdminData.bindDateFilter(search, period, sorter, sessions, "logged_in_at");
        toolbar.add(period, BorderLayout.EAST);
        surface.add(toolbar, BorderLayout.NORTH);
        surface.add(AdminTheme.scroll(table), BorderLayout.CENTER);
        surface.add(new JLabel("Hiển thị " + sessions.size() + " phiên đăng nhập gần nhất · Dữ liệu Supabase"), BorderLayout.SOUTH);
        add(AdminTheme.page("Lịch sử đăng nhập", "Theo dõi thiết bị, IP và thời điểm truy cập tài khoản.", surface));
    }

    static void bindSearch(JTextField search, TableRowSorter<?> sorter) {
        search.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                String value = search.getText().trim();
                sorter.setRowFilter(value.isEmpty() ? null : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(value)));
            }
            @Override public void insertUpdate(DocumentEvent event) { update(); }
            @Override public void removeUpdate(DocumentEvent event) { update(); }
            @Override public void changedUpdate(DocumentEvent event) { update(); }
        });
    }
}
