package com.javachat.shared.model;

/** Password requirements for self-registration. */
public final class RegistrationPassword {
    private RegistrationPassword() { }

    public static String error(String password) {
        if (password == null || password.isEmpty()) return "Vui lòng nhập mật khẩu.";
        if (password.length() < 8) return "Mật khẩu phải có ít nhất 8 ký tự.";
        if (password.length() > 72) return "Mật khẩu tối đa 72 ký tự.";
        if (password.codePoints().noneMatch(Character::isUpperCase)) return "Cần ít nhất một chữ in hoa.";
        return null;
    }
}
