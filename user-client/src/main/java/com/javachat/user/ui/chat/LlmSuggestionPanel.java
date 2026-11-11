package com.javachat.user.ui.chat;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/** A reviewable AI reply suggestion. Only the user can insert it into the draft. */
public final class LlmSuggestionPanel extends JPanel {
    private static final Color INK = new Color(30, 48, 72);
    private static final Color MUTED = new Color(113, 132, 157);
    private static final Color BORDER = new Color(220, 232, 244);
    private final JLabel status = new JLabel();
    private final JTextArea suggestion = new JTextArea(3, 24);
    private final JButton generate = new JButton("Tạo gợi ý");
    private final JButton insert = new JButton("Chèn vào ô nhập");
    private final JButton close = new JButton("Đóng");
    private Runnable generateAction = () -> { };
    private Runnable insertAction = () -> { };
    private Runnable closeAction = () -> { };

    public LlmSuggestionPanel() {
        super(new BorderLayout(0, 10));
        setBackground(new Color(247, 250, 255));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("✦ Gợi ý trả lời bằng AI");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        title.setForeground(INK);
        heading.add(title, BorderLayout.WEST);
        close.addActionListener(event -> closeAction.run());
        heading.add(close, BorderLayout.EAST);
        add(heading, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        status.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        status.setForeground(MUTED);
        status.setAlignmentX(LEFT_ALIGNMENT);
        content.add(status);
        content.add(Box.createVerticalStrut(8));
        suggestion.setEditable(false);
        suggestion.setLineWrap(true);
        suggestion.setWrapStyleWord(true);
        suggestion.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        suggestion.setForeground(INK);
        suggestion.setBackground(Color.WHITE);
        suggestion.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JScrollPane suggestionScroll = new JScrollPane(suggestion);
        suggestionScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        suggestionScroll.setPreferredSize(new Dimension(200, 74));
        suggestionScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        suggestionScroll.setAlignmentX(LEFT_ALIGNMENT);
        content.add(suggestionScroll);
        add(content, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        generate.addActionListener(event -> generateAction.run());
        insert.addActionListener(event -> insertAction.run());
        actions.add(generate);
        actions.add(insert);
        add(actions, BorderLayout.SOUTH);
        reset(true);
    }

    public void setGenerateAction(Runnable action) { generateAction = action; }
    public void setInsertAction(Runnable action) { insertAction = action; }
    public void setCloseAction(Runnable action) { closeAction = action; }

    public void reset(boolean available) {
        suggestion.setText("");
        status.setText(available ? "Bấm Tạo gợi ý để dùng các tin nhắn gần đây làm ngữ cảnh."
                : "Chưa kết nối dịch vụ gợi ý AI.");
        generate.setEnabled(available);
        insert.setEnabled(false);
    }

    public void setLoading() {
        status.setText("Đang tạo gợi ý...");
        suggestion.setText("");
        generate.setEnabled(false);
        insert.setEnabled(false);
    }

    public void setSuggestion(String text, String model) {
        suggestion.setText(text == null ? "" : text.strip());
        suggestion.setCaretPosition(0);
        status.setText(model == null || model.isBlank() ? "Đọc lại trước khi gửi."
                : "Nguồn: " + model + " · Đọc lại trước khi gửi.");
        generate.setEnabled(true);
        insert.setEnabled(!suggestion.getText().isBlank());
    }

    public void setError(String message) {
        status.setText(message == null || message.isBlank() ? "Không thể tạo gợi ý. Hãy thử lại." : message);
        generate.setEnabled(true);
        insert.setEnabled(false);
    }

    public String suggestion() { return suggestion.getText(); }
}
