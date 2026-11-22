package com.javachat.admin.ui;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.time.temporal.ChronoUnit;

/** Small conversion layer for the admin JSON snapshot. */
final class AdminData {
    private static final ZoneId LOCAL = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM");

    private AdminData() { }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> rows(Map<String, Object> snapshot, String key) {
        Object value = snapshot.get(key);
        if (!(value instanceof List<?> list)) throw new IllegalArgumentException("Thiếu dữ liệu: " + key);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) result.add((Map<String, Object>) item);
        return result;
    }

    static String text(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "—" : String.valueOf(value);
    }

    static long number(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value instanceof Number number ? number.longValue() : 0;
    }

    static String time(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return "—";
        try { return TIME.format(Instant.parse(String.valueOf(value)).atZone(LOCAL)); }
        catch (RuntimeException error) { return String.valueOf(value); }
    }

    static String day(Map<String, Object> row) {
        return DATE.format(LocalDate.parse(text(row, "day")));
    }

    static String accountStatus(Map<String, Object> row) {
        return switch (text(row, "status")) {
            case "ACTIVE" -> "Hoạt động";
            case "BLOCKED" -> "Bị khóa";
            default -> text(row, "status");
        };
    }

    static Object[][] table(List<Map<String, Object>> source, java.util.function.Function<Map<String, Object>, Object[]> mapper) {
        return source.stream().map(mapper).toArray(Object[][]::new);
    }

    static void bindDateFilter(JTextField search, JComboBox<String> period, TableRowSorter<?> sorter,
                               List<Map<String, Object>> source, String timeKey) {
        Runnable update = () -> {
            String query = search.getText().trim().toLowerCase(java.util.Locale.ROOT);
            int days = switch (period.getSelectedIndex()) { case 1 -> 7; case 2 -> 30; default -> 0; };
            Instant threshold = days == 0 ? Instant.MIN : Instant.now().minus(days, ChronoUnit.DAYS);
            sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
                @Override public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                    int row = entry.getIdentifier();
                    if (row >= source.size()) return false;
                    if (days != 0) {
                        try {
                            if (Instant.parse(text(source.get(row), timeKey)).isBefore(threshold)) return false;
                        } catch (RuntimeException error) { return false; }
                    }
                    if (query.isEmpty()) return true;
                    for (int column = 0; column < entry.getValueCount(); column++) {
                        if (String.valueOf(entry.getValue(column)).toLowerCase(java.util.Locale.ROOT).contains(query))
                            return true;
                    }
                    return false;
                }
            });
        };
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { update.run(); }
            @Override public void removeUpdate(DocumentEvent event) { update.run(); }
            @Override public void changedUpdate(DocumentEvent event) { update.run(); }
        });
        period.addActionListener(event -> update.run());
    }
}
