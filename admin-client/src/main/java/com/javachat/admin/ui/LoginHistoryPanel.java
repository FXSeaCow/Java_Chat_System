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

final class LoginHistoryPanel extends JPanel {
    LoginHistoryPanel() {
        setLayout(new BorderLayout());
        JTable table = AdminTheme.table(new Object[][] {
                {"Nguyễn Minh Anh", "minhanh@example.com", "Chrome · Windows", "113.161.44.21", "21/11/2026 09:42", "Đang hoạt động"},
                {"Trần Gia Huy", "giahuy@example.com", "Java Client · Windows", "14.177.82.104", "21/11/2026 09:18", "Đã đăng xuất"},
                {"Lê Hoàng Nam", "nam@example.com", "Safari · macOS", "42.118.91.18", "21/11/2026 08:51", "Đang hoạt động"},
                {"Nguyễn Thu Thảo", "thao@example.com", "Java Client · Linux", "171.244.22.10", "20/11/2026 22:14", "Hết hạn"},
                {"Phạm Đức Long", "long@example.com", "Chrome · Android", "27.72.118.33", "20/11/2026 20:07", "Đã đăng xuất"}
        }, new Object[] {"Người dùng", "Email", "Thiết bị", "Địa chỉ IP", "Đăng nhập lúc", "Trạng thái"});
        TableRowSorter<?> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        JPanel surface = AdminTheme.surface();
        surface.setLayout(new BorderLayout(0, 14));
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JTextField search = AdminTheme.searchField("Tìm người dùng, email hoặc địa chỉ IP");
        search.setToolTipText("Tìm người dùng, email hoặc địa chỉ IP");
        bindSearch(search, sorter);
        toolbar.add(search, BorderLayout.WEST);
        JComboBox<String> period = new JComboBox<>(new String[] {"7 ngày gần đây", "30 ngày gần đây", "Tất cả"});
        period.setFont(AdminTheme.BODY);
        period.getAccessibleContext().setAccessibleName("Khoảng thời gian");
        toolbar.add(period, BorderLayout.EAST);
        surface.add(toolbar, BorderLayout.NORTH);
        surface.add(AdminTheme.scroll(table), BorderLayout.CENTER);
        surface.add(new JLabel("Hiển thị 5 phiên đăng nhập gần nhất · Dữ liệu mẫu giao diện"), BorderLayout.SOUTH);
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
