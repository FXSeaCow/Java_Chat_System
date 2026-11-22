package com.javachat.admin.ui;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

/** Live account list with reversible USER account moderation. */
final class UserManagementPanel extends JPanel {
    UserManagementPanel(List<Map<String, Object>> users, BiConsumer<Long, String> onStatusChange) {
        setLayout(new BorderLayout());
        JTable table = AdminTheme.table(AdminData.table(users, row -> new Object[] {
                AdminData.text(row, "display_name"), AdminData.text(row, "username"),
                AdminData.text(row, "email"), AdminData.time(row, "created_at"),
                AdminData.time(row, "last_login"), AdminData.accountStatus(row)
        }), new Object[] {"Họ tên", "Tên đăng nhập", "Email", "Ngày tạo", "Đăng nhập gần nhất", "Trạng thái"});
        AdminTheme.columnWidths(table, 155, 140, 200, 155, 155, 115);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        JPanel surface = AdminTheme.surface();
        surface.setLayout(new BorderLayout(0, 14));
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JTextField search = AdminTheme.searchField("Tìm họ tên, email hoặc tên đăng nhập");
        toolbar.add(search, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JComboBox<String> statusFilter = new JComboBox<>(new String[] {"Tất cả", "Hoạt động", "Bị khóa"});
        AdminTheme.styleComboBox(statusFilter);
        JButton block = AdminTheme.button("Khóa tài khoản", false);
        JButton unblock = AdminTheme.button("Mở khóa", true);
        block.setEnabled(false);
        unblock.setEnabled(false);
        actions.add(statusFilter);
        actions.add(block);
        actions.add(unblock);
        toolbar.add(actions, BorderLayout.EAST);

        Runnable updateFilter = () -> {
            String query = search.getText().trim().toLowerCase(Locale.ROOT);
            String selected = statusFilter.getSelectedIndex() == 1 ? "ACTIVE"
                    : statusFilter.getSelectedIndex() == 2 ? "BLOCKED" : "";
            sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
                @Override public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                    Map<String, Object> user = users.get(entry.getIdentifier());
                    if (!selected.isEmpty() && !selected.equals(AdminData.text(user, "status"))) return false;
                    if (query.isEmpty()) return true;
                    return (AdminData.text(user, "display_name") + " "
                            + AdminData.text(user, "username") + " "
                            + AdminData.text(user, "email")).toLowerCase(Locale.ROOT).contains(query);
                }
            });
        };
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { updateFilter.run(); }
            @Override public void removeUpdate(DocumentEvent event) { updateFilter.run(); }
            @Override public void changedUpdate(DocumentEvent event) { updateFilter.run(); }
        });
        statusFilter.addActionListener(event -> updateFilter.run());
        table.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) return;
            int selected = table.getSelectedRow();
            String status = selected < 0 ? "" : AdminData.text(
                    users.get(table.convertRowIndexToModel(selected)), "status");
            block.setEnabled("ACTIVE".equals(status));
            unblock.setEnabled("BLOCKED".equals(status));
        });
        block.addActionListener(event -> changeStatus(table, users, onStatusChange, "BLOCKED"));
        unblock.addActionListener(event -> changeStatus(table, users, onStatusChange, "ACTIVE"));

        surface.add(toolbar, BorderLayout.NORTH);
        surface.add(AdminTheme.scroll(table), BorderLayout.CENTER);
        long blocked = users.stream().filter(row -> "BLOCKED".equals(AdminData.text(row, "status"))).count();
        surface.add(new JLabel("Hiển thị " + users.size() + " tài khoản gần nhất · " + blocked
                + " bị khóa · Khóa tài khoản sẽ kết thúc các phiên đăng nhập hiện tại."), BorderLayout.SOUTH);
        add(AdminTheme.page("Quản lý người dùng",
                "Tìm kiếm, kiểm tra và thay đổi trạng thái tài khoản người dùng.", surface));
    }

    private void changeStatus(JTable table, List<Map<String, Object>> users,
                              BiConsumer<Long, String> onStatusChange, String status) {
        int selected = table.getSelectedRow();
        if (selected < 0) return;
        Map<String, Object> user = users.get(table.convertRowIndexToModel(selected));
        if ("BLOCKED".equals(status)) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Khóa tài khoản " + AdminData.text(user, "display_name")
                            + "? Các phiên đăng nhập hiện tại sẽ kết thúc.",
                    "Xác nhận khóa tài khoản", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) return;
        }
        onStatusChange.accept(AdminData.number(user, "id"), status);
    }
}
