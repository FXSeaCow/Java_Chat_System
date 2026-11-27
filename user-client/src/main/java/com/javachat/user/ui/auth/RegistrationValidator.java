package com.javachat.user.ui.auth;

import com.javachat.shared.model.RegistrationPassword;
import java.util.regex.Pattern;

/** Validation shared by the registration form and its tests. */
final class RegistrationValidator {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private RegistrationValidator() { }

    static Errors validate(String fullName, String email, String password, String confirmation) {
        String normalizedName = fullName == null ? "" : fullName.strip();
        String normalizedEmail = email == null ? "" : email.strip();
        String safePassword = password == null ? "" : password;
        String safeConfirmation = confirmation == null ? "" : confirmation;

        String nameError = normalizedName.isEmpty() ? "Vui lòng nhập họ và tên." : null;
        String emailError = normalizedEmail.isEmpty() ? "Vui lòng nhập email."
                : !EMAIL.matcher(normalizedEmail).matches() ? "Vui lòng nhập email hợp lệ." : null;
        String passwordError = RegistrationPassword.error(safePassword);
        String confirmationError = safeConfirmation.isEmpty() ? "Vui lòng xác nhận mật khẩu."
                : !safeConfirmation.equals(safePassword) ? "Mật khẩu xác nhận không khớp." : null;
        return new Errors(normalizedName, normalizedEmail, nameError, emailError,
                passwordError, confirmationError);
    }

    record Errors(String fullName, String email, String fullNameError, String emailError,
                  String passwordError, String confirmationError) {
        boolean valid() {
            return fullNameError == null && emailError == null
                    && passwordError == null && confirmationError == null;
        }
    }
}
