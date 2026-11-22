package com.javachat.admin.ui;

import com.javachat.admin.net.AdminApiClient;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.Map;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/** Admin sign-in; credentials are sent only to the role-checked loopback API. */
public final class AdminLoginFrame extends JFrame {
    private final JTextField login = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JLabel error = new JLabel(" ");
    private final JButton submit = AdminTheme.button("Đăng nhập quản trị", true);

    public AdminLoginFrame() {
        super("E-Chat Desktop - Đăng nhập quản trị");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(490, 410);
        setMinimumSize(new Dimension(450, 380));
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AdminTheme.BACKGROUND);
        root.setBorder(new EmptyBorder(28, 38, 28, 38));
        JLabel heading = new JLabel("E-Chat · Quản trị hệ thống");
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 23));
        heading.setForeground(AdminTheme.TEXT);
        root.add(heading, BorderLayout.NORTH);
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(28, 0, 0, 0));
        form.add(label("Email hoặc tên đăng nhập"));
        form.add(Box.createVerticalStrut(7));
        styleField(login);
        form.add(login);
        form.add(Box.createVerticalStrut(17));
        form.add(label("Mật khẩu"));
        form.add(Box.createVerticalStrut(7));
        styleField(password);
        form.add(password);
        form.add(Box.createVerticalStrut(12));
        error.setForeground(AdminTheme.DANGER);
        error.setFont(AdminTheme.BODY);
        form.add(error);
        form.add(Box.createVerticalStrut(8));
        submit.setAlignmentX(Component.LEFT_ALIGNMENT);
        submit.addActionListener(event -> signIn());
        form.add(submit);
        root.add(form, BorderLayout.CENTER);
        setContentPane(root);
        getRootPane().setDefaultButton(submit);
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(AdminTheme.MEDIUM);
        label.setForeground(AdminTheme.TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static void styleField(JTextField field) {
        field.setFont(AdminTheme.BODY);
        field.setForeground(AdminTheme.TEXT);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AdminTheme.BORDER), new EmptyBorder(8, 12, 8, 12)));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void signIn() {
        String username = login.getText().trim();
        char[] secret = password.getPassword();
        if (username.isEmpty() || secret.length == 0) {
            error.setText("Hãy nhập email/tên đăng nhập và mật khẩu.");
            Arrays.fill(secret, '\0');
            return;
        }
        submit.setEnabled(false);
        error.setText("Đang kết nối server...");
        new SwingWorker<Session, Void>() {
            @Override protected Session doInBackground() throws Exception {
                try {
                    AdminApiClient api = new AdminApiClient();
                    api.login(username, new String(secret));
                    Map<String, Object> snapshot = api.snapshot();
                    if (!snapshot.containsKey("users"))
                        throw new IOException("Server đang chạy bản cũ. Hãy khởi động lại run-server.ps1.");
                    return new Session(api, snapshot);
                } finally { Arrays.fill(secret, '\0'); }
            }
            @Override protected void done() {
                submit.setEnabled(true);
                try {
                    Session session = get();
                    dispose();
                    new AdminFrame(session.api(), session.snapshot()).setVisible(true);
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException failure) {
                    error.setText(failure.getCause().getMessage());
                }
                password.setText("");
            }
        }.execute();
    }

    private record Session(AdminApiClient api, Map<String, Object> snapshot) { }
}
