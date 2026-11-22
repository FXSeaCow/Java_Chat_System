package com.javachat.user.ui.account;

import com.javachat.shared.dto.AuthDtos.ChangePasswordRequest;
import com.javachat.user.ui.chat.ChatTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

final class PasswordChangeDialog extends JDialog {
    private static final Color ERROR = new Color(175, 47, 47);
    private final JPasswordField current = passwordField();
    private final JPasswordField replacement = passwordField();
    private final JPasswordField confirmation = passwordField();
    private final JCheckBox reveal = new JCheckBox("Hiện mật khẩu");
    private final JLabel error = new JLabel(" ");
    private final AccountButton cancel = new AccountButton("Hủy", AccountButton.Style.SECONDARY);
    private final AccountButton submit = new AccountButton("Cập nhật mật khẩu", AccountButton.Style.PRIMARY);
    private final JButton close = new JButton("×");
    private final Consumer<ChangePasswordRequest> listener;
    private boolean pending;

    PasswordChangeDialog(Window owner, Consumer<ChangePasswordRequest> listener) {
        super(owner, "Đổi mật khẩu", Dialog.ModalityType.APPLICATION_MODAL);
        this.listener = listener;
        setUndecorated(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setContentPane(buildContent());
        pack();
        setMinimumSize(new Dimension(470, getHeight()));
        setSize(Math.max(470, getWidth()), getHeight());
        setLocationRelativeTo(owner);

        cancel.addActionListener(event -> dismiss());
        close.addActionListener(event -> dismiss());
        submit.addActionListener(event -> submit());
        getRootPane().setDefaultButton(submit);
        getRootPane().registerKeyboardAction(event -> dismiss(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        SwingUtilities.invokeLater(current::requestFocusInWindow);
    }

    private JPanel buildContent() {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(ChatTheme.MILK);
        shell.setBorder(BorderFactory.createLineBorder(ChatTheme.BORDER));

        JPanel chrome = new JPanel(new BorderLayout());
        chrome.setBackground(ChatTheme.TEAL_PALE);
        chrome.setBorder(BorderFactory.createEmptyBorder(6, 23, 6, 12));
        JLabel section = new JLabel("BẢO MẬT TÀI KHOẢN");
        section.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        section.setForeground(ChatTheme.TEAL_DARK);
        chrome.add(section, BorderLayout.WEST);
        close.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
        close.setForeground(ChatTheme.INK);
        close.setContentAreaFilled(false);
        close.setBorder(BorderFactory.createEmptyBorder(1, 10, 1, 10));
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setToolTipText("Đóng hộp thoại");
        close.getAccessibleContext().setAccessibleName("Đóng hộp thoại");
        chrome.add(close, BorderLayout.EAST);
        MouseAdapter drag = new MouseAdapter() {
            private Point start;
            @Override public void mousePressed(MouseEvent event) { start = event.getLocationOnScreen(); }
            @Override public void mouseDragged(MouseEvent event) {
                if (start == null) return;
                Point now = event.getLocationOnScreen();
                setLocation(getX() + now.x - start.x, getY() + now.y - start.y);
                start = now;
            }
        };
        chrome.addMouseListener(drag);
        chrome.addMouseMotionListener(drag);
        section.addMouseListener(drag);
        section.addMouseMotionListener(drag);
        shell.add(chrome, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(25, 30, 27, 30));
        JLabel title = new JLabel("Đổi mật khẩu");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 23));
        title.setForeground(ChatTheme.INK);
        title.setAlignmentX(LEFT_ALIGNMENT);
        JLabel hint = new JLabel("Dùng mật khẩu hiện tại để xác nhận tài khoản của bạn.");
        hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        hint.setForeground(ChatTheme.MUTED);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        body.add(title);
        body.add(Box.createVerticalStrut(6));
        body.add(hint);
        body.add(Box.createVerticalStrut(23));
        body.add(row("Mật khẩu hiện tại", current));
        body.add(Box.createVerticalStrut(15));
        body.add(row("Mật khẩu mới", replacement));
        body.add(Box.createVerticalStrut(15));
        body.add(row("Xác nhận mật khẩu mới", confirmation));
        body.add(Box.createVerticalStrut(12));

        reveal.setOpaque(false);
        reveal.setForeground(ChatTheme.INK);
        reveal.setAlignmentX(LEFT_ALIGNMENT);
        reveal.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        char echo = current.getEchoChar();
        reveal.addActionListener(event -> {
            char value = reveal.isSelected() ? (char) 0 : echo;
            current.setEchoChar(value);
            replacement.setEchoChar(value);
            confirmation.setEchoChar(value);
        });
        body.add(reveal);
        body.add(Box.createVerticalStrut(8));
        JLabel guidance = new JLabel("Mật khẩu mới cần từ 8 đến 72 ký tự.");
        guidance.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        guidance.setForeground(ChatTheme.MUTED);
        guidance.setAlignmentX(LEFT_ALIGNMENT);
        body.add(guidance);
        body.add(Box.createVerticalStrut(12));
        error.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        error.setForeground(ERROR);
        error.setAlignmentX(LEFT_ALIGNMENT);
        error.setPreferredSize(new Dimension(400, 20));
        body.add(error);
        body.add(Box.createVerticalStrut(16));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        actions.add(cancel);
        actions.add(submit);
        body.add(actions);
        body.setPreferredSize(new Dimension(468, body.getPreferredSize().height + 14));
        shell.add(body, BorderLayout.CENTER);
        return shell;
    }

    void showError(String message) {
        setPending(false);
        error.setText(message == null ? "Không đổi được mật khẩu." : message);
    }

    void setPending(boolean value) {
        pending = value;
        for (JPasswordField field : new JPasswordField[] {current, replacement, confirmation}) {
            field.setEnabled(!value);
        }
        cancel.setEnabled(!value);
        close.setEnabled(!value);
        reveal.setEnabled(!value);
        submit.setEnabled(!value);
        submit.setText(value ? "Đang cập nhật..." : "Cập nhật mật khẩu");
    }

    private void dismiss() {
        if (!pending) dispose();
    }

    private void submit() {
        if (pending) return;
        String oldPassword = new String(current.getPassword());
        String newPassword = new String(replacement.getPassword());
        String confirmed = new String(confirmation.getPassword());
        if (oldPassword.isEmpty()) {
            showError("Vui lòng nhập mật khẩu hiện tại.");
            current.requestFocusInWindow();
        } else if (newPassword.length() < 8 || newPassword.length() > 72) {
            showError("Mật khẩu mới cần có từ 8 đến 72 ký tự.");
            replacement.requestFocusInWindow();
        } else if (newPassword.equals(oldPassword)) {
            showError("Mật khẩu mới phải khác mật khẩu hiện tại.");
            replacement.requestFocusInWindow();
        } else if (!newPassword.equals(confirmed)) {
            showError("Xác nhận mật khẩu mới chưa khớp.");
            confirmation.requestFocusInWindow();
        } else {
            error.setText(" ");
            setPending(true);
            listener.accept(new ChangePasswordRequest(oldPassword, newPassword));
        }
    }

    private static JPanel row(String labelText, JPasswordField field) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        JLabel label = new JLabel(labelText);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        label.setForeground(ChatTheme.INK);
        label.setLabelFor(field);
        label.setAlignmentX(LEFT_ALIGNMENT);
        field.getAccessibleContext().setAccessibleName(labelText);
        field.setAlignmentX(LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        row.add(label);
        row.add(Box.createVerticalStrut(6));
        row.add(field);
        return row;
    }

    private static JPasswordField passwordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(390, 44));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ChatTheme.BORDER),
                BorderFactory.createEmptyBorder(9, 11, 9, 11)));
        return field;
    }
}
