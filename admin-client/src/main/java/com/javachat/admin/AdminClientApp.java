package com.javachat.admin;

import com.javachat.admin.ui.AdminLoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class AdminClientApp {
    private AdminClientApp() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 12);
            UIManager.put("TextComponent.arc", 12);
            new AdminLoginFrame().setVisible(true);
        });
    }
}
