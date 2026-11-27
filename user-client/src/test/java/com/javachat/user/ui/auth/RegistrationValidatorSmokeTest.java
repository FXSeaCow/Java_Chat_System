package com.javachat.user.ui.auth;

/** Dependency-free checks for the JC-64 registration acceptance rules. */
public final class RegistrationValidatorSmokeTest {
    private RegistrationValidatorSmokeTest() { }

    public static void main(String[] args) {
        var empty = RegistrationValidator.validate("  ", "  ", "", "");
        check("Vui lòng nhập họ và tên.".equals(empty.fullNameError()), "empty name");
        check("Vui lòng nhập email.".equals(empty.emailError()), "empty email");
        check("Vui lòng nhập mật khẩu.".equals(empty.passwordError()), "empty password");
        check("Vui lòng xác nhận mật khẩu.".equals(empty.confirmationError()), "empty confirmation");

        var invalid = RegistrationValidator.validate("  Nguyễn An  ", " no-at-sign ",
                "1234567", "12345678");
        check("Nguyễn An".equals(invalid.fullName()), "trim name");
        check("no-at-sign".equals(invalid.email()), "trim email");
        check("Vui lòng nhập email hợp lệ.".equals(invalid.emailError()), "invalid email");
        check("Mật khẩu phải có ít nhất 8 ký tự.".equals(invalid.passwordError()), "short password");
        check("Mật khẩu xác nhận không khớp.".equals(invalid.confirmationError()), "mismatch");

        check("Cần ít nhất một chữ in hoa.".equals(RegistrationValidator.validate(
                "Nguyễn An", "an@example.com", "abcdef12", "abcdef12").passwordError()), "missing uppercase");
        check(RegistrationValidator.validate("Nguyễn An", "an@example.com",
                "Đbcdef12", "Đbcdef12").valid(), "valid form with Unicode uppercase");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
