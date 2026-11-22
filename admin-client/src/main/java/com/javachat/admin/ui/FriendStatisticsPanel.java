package com.javachat.admin.ui;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.text.Collator;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Accepted friendships: direct contacts and distinct contacts reachable in two steps. */
final class FriendStatisticsPanel extends JPanel {
    FriendStatisticsPanel(List<Map<String, Object>> statistics) {
        setLayout(new BorderLayout());
        JTable table = AdminTheme.table(AdminData.table(statistics, row -> new Object[] {
                AdminData.text(row, "display_name"), AdminData.text(row, "username"),
                AdminData.text(row, "created_at"), AdminData.number(row, "direct_count"),
                AdminData.number(row, "network_count")
        }), new Object[] {"Họ tên", "Tên đăng nhập", "Ngày tạo", "Bạn trực tiếp", "Gồm bạn của bạn"});
        AdminTheme.columnWidths(table, 210, 165, 175, 145, 170);
        DefaultTableCellRenderer dateRenderer = new DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) {
                super.setValue(value == null ? "—" : AdminData.formatTime(String.valueOf(value)));
            }
        };
        dateRenderer.setBorder(new EmptyBorder(0, 12, 0, 12));
        table.getColumnModel().getColumn(2).setCellRenderer(dateRenderer);

        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        Collator vietnamese = Collator.getInstance(Locale.forLanguageTag("vi-VN"));
        vietnamese.setStrength(Collator.PRIMARY);
        sorter.setComparator(0, (left, right) -> vietnamese.compare(left, right));
        sorter.setComparator(2, Comparator.comparing(Object::toString));
        Comparator<Object> numbers = Comparator.comparingLong(value -> ((Number) value).longValue());
        sorter.setComparator(3, numbers);
        sorter.setComparator(4, numbers);
        table.setRowSorter(sorter);

        JPanel surface = AdminTheme.surface();
        surface.setLayout(new BorderLayout(0, 14));
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        JTextField nameFilter = AdminTheme.searchField("Lọc theo họ tên");
        toolbar.add(nameFilter, BorderLayout.WEST);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);
        JComboBox<String> sort = new JComboBox<>(new String[] {
                "Tên A–Z", "Tên Z–A", "Mới nhất", "Cũ nhất"
        });
        AdminTheme.styleComboBox(sort);
        sort.getAccessibleContext().setAccessibleName("Sắp xếp theo tên hoặc ngày tạo");
        JComboBox<String> comparison = new JComboBox<>(new String[] {
                "Mọi số bạn", "Bằng", "Nhỏ hơn", "Lớn hơn"
        });
        AdminTheme.styleComboBox(comparison);
        comparison.getAccessibleContext().setAccessibleName("So sánh số bạn trực tiếp");
        JSpinner count = new JSpinner(new SpinnerNumberModel(0, 0, 1_000_000, 1));
        count.setFont(AdminTheme.BODY);
        count.setPreferredSize(new Dimension(82, 40));
        count.setEnabled(false);
        count.getAccessibleContext().setAccessibleName("Số bạn trực tiếp để lọc");
        controls.add(sort);
        controls.add(comparison);
        controls.add(count);
        toolbar.add(controls, BorderLayout.EAST);

        Runnable applyFilter = () -> {
            String query = normalize(nameFilter.getText().trim());
            int mode = comparison.getSelectedIndex();
            long target = ((Number) count.getValue()).longValue();
            count.setEnabled(mode != 0);
            sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
                @Override public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                    Map<String, Object> user = statistics.get(entry.getIdentifier());
                    if (!normalize(AdminData.text(user, "display_name")).contains(query)) return false;
                    long direct = AdminData.number(user, "direct_count");
                    return switch (mode) {
                        case 1 -> direct == target;
                        case 2 -> direct < target;
                        case 3 -> direct > target;
                        default -> true;
                    };
                }
            });
        };
        nameFilter.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { applyFilter.run(); }
            @Override public void removeUpdate(DocumentEvent event) { applyFilter.run(); }
            @Override public void changedUpdate(DocumentEvent event) { applyFilter.run(); }
        });
        comparison.addActionListener(event -> applyFilter.run());
        count.addChangeListener(event -> applyFilter.run());
        sort.addActionListener(event -> {
            int column = sort.getSelectedIndex() < 2 ? 0 : 2;
            SortOrder direction = sort.getSelectedIndex() == 1 || sort.getSelectedIndex() == 2
                    ? SortOrder.DESCENDING : SortOrder.ASCENDING;
            sorter.setSortKeys(List.of(new RowSorter.SortKey(column, direction)));
        });
        sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));

        surface.add(toolbar, BorderLayout.NORTH);
        surface.add(AdminTheme.scroll(table), BorderLayout.CENTER);
        JLabel note = new JLabel("Hiển thị " + statistics.size()
                + " người dùng · Cột cuối tính cả bạn trực tiếp và bạn của bạn, mỗi người một lần; không tính chính mình.");
        note.setFont(AdminTheme.BODY);
        note.setForeground(AdminTheme.MUTED);
        surface.add(note, BorderLayout.SOUTH);
        add(AdminTheme.page("Thống kê bạn bè",
                "Số bạn trực tiếp và số người trong mạng bạn bè hai bước.", surface));
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }
}
