package com.javachat.admin.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/** Add/edit user form. Status changes remain separate moderation actions. */
final class AdminUserDialog extends JDialog {
    record Input(String displayName, String username, String email, String password) { }

    private final boolean creating;
    private final JTextField name = new JTextField();
    private final JTextField username = new JTextField();
    private final JTextField email = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JLabel error = new JLabel(" ");
    private Input result;

    private AdminUserDialog(Window owner, Map<String, Object> current) {
        super(owner, current == null ? "Thêm người dùng" : "Sửa người dùng", ModalityType.APPLICATION_MODAL);
        creating = current == null;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(510, creating ? 525 : 445);
        setMinimumSize(new Dimension(470, creating ? 510 : 430));
        setLocationRelativeTo(owner);
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(AdminTheme.BACKGROUND);
        root.setBorder(new EmptyBorder(28, 32, 26, 32));
        JLabel title = new JLabel(creating ? "Thêm người dùng" : "Sửa thông tin người dùng");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        title.setForeground(AdminTheme.TEXT);
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        addField(form, "Họ tên *", name);
        addField(form, "Tên đăng nhập *", username);
        addField(form, "Email *", email);
        if (creating) addField(form, "Mật khẩu * (ít nhất 8 ký tự)", password);
        error.setForeground(AdminTheme.DANGER);
        error.setFont(AdminTheme.BODY);
        error.setAlignmentX(LEFT_ALIGNMENT);
        form.add(error);
        root.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        JButton cancel = AdminTheme.button("Hủy", false);
        JButton save = AdminTheme.button(creating ? "Thêm người dùng" : "Lưu thay đổi", true);
        cancel.addActionListener(event -> dispose());
        save.addActionListener(event -> submit());
        actions.add(cancel);
        actions.add(save);
        root.add(actions, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(save);
        if (current != null) {
            name.setText(AdminData.text(current, "display_name"));
            username.setText(AdminData.text(current, "username"));
            email.setText(AdminData.text(current, "email"));
        }
    }

    static Input showCreate(Window owner) {
        AdminUserDialog dialog = new AdminUserDialog(owner, null);
        dialog.setVisible(true);
        return dialog.result;
    }

    static Input showEdit(Window owner, Map<String, Object> current) {
        AdminUserDialog dialog = new AdminUserDialog(owner, current);
        dialog.setVisible(true);
        return dialog.result;
    }

    private static void addField(JPanel form, String labelText, JTextField field) {
        JLabel label = new JLabel(labelText, SwingConstants.LEFT);
        label.setFont(AdminTheme.MEDIUM);
        label.setForeground(AdminTheme.TEXT);
        label.setAlignmentX(LEFT_ALIGNMENT);
        form.add(label);
        form.add(Box.createVerticalStrut(5));
        field.setFont(AdminTheme.BODY);
        field.setForeground(AdminTheme.TEXT);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AdminTheme.BORDER), new EmptyBorder(9, 12, 9, 12)));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 41));
        field.setAlignmentX(LEFT_ALIGNMENT);
        form.add(field);
        form.add(Box.createVerticalStrut(16));
    }

    private void submit() {
        String displayName = name.getText().trim();
        String login = username.getText().trim();
        String address = email.getText().trim().toLowerCase(Locale.ROOT);
        char[] secret = creating ? password.getPassword() : new char[0];
        try {
            if (displayName.length() < 2 || displayName.length() > 100) {
                error.setText("Họ tên phải có từ 2 đến 100 ký tự.");
                return;
            }
            if (!login.matches("[A-Za-z0-9._-]{3,50}")) {
                error.setText("Tên đăng nhập cần 3–50 ký tự chữ, số, dấu chấm, _ hoặc -.");
                return;
            }
            if (address.length() > 255 || !address.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                error.setText("Email không hợp lệ.");
                return;
            }
            if (creating && (secret.length < 8 || secret.length > 72)) {
                error.setText("Mật khẩu phải có từ 8 đến 72 ký tự.");
                return;
            }
            result = new Input(displayName, login, address, creating ? new String(secret) : null);
            dispose();
        } finally { Arrays.fill(secret, '\0'); }
    }
}
