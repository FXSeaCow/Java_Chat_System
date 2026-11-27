package com.javachat.user.ui.auth;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import com.javachat.user.net.LiveApiClient.ApiException;

/** Warm, mascot-led authentication window for the desktop client. */
public final class LoginFrame extends JFrame {
    private static final Color CANVAS = new Color(250, 248, 245);
    private static final Color HERO_BG = new Color(255, 252, 247);
    private static final Color SURFACE = Color.WHITE;
    private static final Color INK = new Color(35, 32, 30);
    private static final Color MUTED = new Color(105, 101, 98);
    private static final Color BORDER = new Color(226, 222, 218);
    private static final Color FIELD_BG = new Color(249, 248, 247);
    private static final Color ORANGE = new Color(235, 101, 28);
    private static final Color ORANGE_DARK = new Color(204, 75, 12);
    private static final Color FOCUS = new Color(43, 108, 176);
    private static final Color ERROR = new Color(184, 40, 40);
    private static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 15);
    private static final Font MEDIUM = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    private static final int AUTH_CARD_DEFAULT_HEIGHT = 360;
    private static final int AUTH_CARD_FORGOT_HEIGHT = 230;

    private final Runnable onAuthenticated;
    private final LoginHandler loginHandler;
    private final RegistrationHandler registrationHandler;
    private final OtpSender otpSender;
    private final PasswordResetHandler passwordResetHandler;
    private boolean registrationPending;
    private boolean loginPending;
    private final JTextField loginField = new RoundedTextField("you@example.com");
    private final JPasswordField passwordField = new RoundedPasswordField();
    private final JLabel loginError = errorLabel();
    private final JLabel passwordError = errorLabel();
    private final JTextField fullNameField = new RoundedTextField("Nguyễn Minh Anh");
    private final JTextField registrationEmailField = new RoundedTextField("you@example.com");
    private final JPasswordField registrationPasswordField = new RoundedPasswordField();
    private final JPasswordField confirmPasswordField = new RoundedPasswordField();
    private final JLabel fullNameError = errorLabel();
    private final JLabel registrationEmailError = errorLabel();
    private final JLabel registrationPasswordError = errorLabel();
    private final JLabel confirmPasswordError = errorLabel();
    private final JTextField forgotEmailField = new RoundedTextField("you@example.com");
    private final JLabel forgotEmailError = errorLabel();
    private final CardLayout authCardLayout = new CardLayout();
    private final JPanel authCards = new JPanel(authCardLayout);
    private final JLabel formStatus = new JLabel(" ");
    private final RoundedButton submitButton = new RoundedButton("Đăng nhập", true);
    private final PawModeSwitch modeSwitch = new PawModeSwitch();
    private final JLabel heading = new JLabel("Chào mừng trở lại!");
    private final JLabel subheading = new JLabel("Đăng nhập để tiếp tục trò chuyện cùng bạn bè.");
    private boolean signUpMode;
    private boolean forgotPasswordMode;

    @FunctionalInterface
    public interface LoginHandler {
        void login(String login, String password) throws Exception;
    }

    @FunctionalInterface
    public interface OtpSender {
        void send(String email) throws Exception;
    }

    @FunctionalInterface
    public interface PasswordResetHandler {
        void request(String email, String otp) throws Exception;
    }

    @FunctionalInterface
    public interface RegistrationHandler {
        long register(String displayName, String email, String password, String otp) throws Exception;
    }

    public LoginFrame(Runnable onAuthenticated, LoginHandler loginHandler, OtpSender otpSender,
                      RegistrationHandler registrationHandler, PasswordResetHandler passwordResetHandler) {
        super("Java Chat - Đăng nhập");
        this.onAuthenticated = Objects.requireNonNull(onAuthenticated);
        this.loginHandler = loginHandler;
        this.otpSender = otpSender;
        this.passwordResetHandler = passwordResetHandler;
        this.registrationHandler = registrationHandler;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Rectangle usableScreen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int windowWidth = Math.min(1_040, Math.max(840, (int) (usableScreen.width * 0.88)));
        int windowHeight = Math.min(650, Math.max(560, (int) (usableScreen.height * 0.88)));
        setSize(windowWidth, windowHeight);
        setMinimumSize(new Dimension(840, 560));
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        getRootPane().setDefaultButton(submitButton);
        getRootPane().registerKeyboardAction(event -> handleEscape(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new GridLayout(1, 2));
        content.setBackground(CANVAS);
        content.add(new HeroPanel());
        content.add(buildFormSide());
        return content;
    }

    private JPanel buildFormSide() {
        JPanel side = new JPanel(new GridBagLayout());
        side.setBackground(CANVAS);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.NONE;
        side.add(buildFormCard(), gbc);
        return side;
    }

    private JPanel buildFormCard() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(420, 550));
        form.setBorder(new EmptyBorder(8, 4, 8, 4));

        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 27));
        heading.setForeground(INK);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        subheading.setFont(BODY);
        subheading.setForeground(MUTED);
        subheading.setAlignmentX(LEFT_ALIGNMENT);

        modeSwitch.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        modeSwitch.setPreferredSize(new Dimension(420, 62));
        modeSwitch.setAlignmentX(LEFT_ALIGNMENT);
        modeSwitch.setSelectionListener(this::setMode);

        form.add(heading);
        form.add(Box.createVerticalStrut(7));
        form.add(subheading);
        form.add(Box.createVerticalStrut(20));
        form.add(modeSwitch);
        form.add(Box.createVerticalStrut(12));
        authCards.setOpaque(false);
        authCards.setAlignmentX(LEFT_ALIGNMENT);
        setAuthCardHeight(AUTH_CARD_DEFAULT_HEIGHT);
        authCards.add(buildLoginCard(), "login");
        authCards.add(buildRegistrationCard(), "register");
        authCards.add(buildForgotPasswordCard(), "forgot");
        form.add(authCards);
        form.add(Box.createVerticalStrut(11));

        submitButton.setAlignmentX(LEFT_ALIGNMENT);
        submitButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        submitButton.addActionListener(event -> submit());
        form.add(submitButton);
        form.add(Box.createVerticalStrut(9));
        formStatus.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        formStatus.setForeground(MUTED);
        formStatus.setAlignmentX(LEFT_ALIGNMENT);
        form.add(formStatus);
        return form;
    }

    private JPanel buildLoginCard() {
        JPanel card = verticalCard();
        RoundedButton google = new RoundedButton("Tiếp tục với Google", false);
        google.setIcon(new GoogleIcon());
        google.setIconTextGap(14);
        google.setAlignmentX(LEFT_ALIGNMENT);
        google.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        google.getAccessibleContext().setAccessibleDescription("Đăng nhập bằng tài khoản Google");
        google.addActionListener(event -> setStatus("Google Sign-In sẽ được kết nối với máy chủ sau.", false));
        card.add(google);
        card.add(Box.createVerticalStrut(10));
        card.add(divider());
        card.add(Box.createVerticalStrut(10));
        card.add(fieldLabel("Email hoặc tên đăng nhập"));
        card.add(Box.createVerticalStrut(5));
        configureField(loginField, "Email hoặc tên đăng nhập");
        card.add(loginField);
        card.add(loginError);

        card.add(fieldLabel("Mật khẩu"));
        card.add(Box.createVerticalStrut(5));
        configureField(passwordField, "Mật khẩu");
        card.add(passwordField);
        card.add(passwordError);

        JCheckBox showPassword = compactCheckBox("Hiện mật khẩu");
        char echo = passwordField.getEchoChar();
        showPassword.addActionListener(event -> passwordField.setEchoChar(
                showPassword.isSelected() ? (char) 0 : echo));

        JPanel passwordActions = new JPanel(new BorderLayout());
        passwordActions.setOpaque(false);
        passwordActions.setAlignmentX(LEFT_ALIGNMENT);
        passwordActions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        passwordActions.add(showPassword, BorderLayout.WEST);
        JButton forgot = linkButton("Quên mật khẩu?");
        forgot.addActionListener(event -> showForgotPassword());
        passwordActions.add(forgot, BorderLayout.EAST);
        card.add(passwordActions);
        return card;
    }

    private JPanel buildForgotPasswordCard() {
        JPanel card = verticalCard();
        JLabel guidance = new JLabel("<html>Nhập email đã đăng ký để nhận mã xác minh<br>và yêu cầu mật khẩu mới.</html>");
        guidance.setFont(BODY);
        guidance.setForeground(MUTED);
        guidance.setAlignmentX(LEFT_ALIGNMENT);
        guidance.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        card.add(guidance);
        card.add(Box.createVerticalStrut(20));
        card.add(fieldLabel("Email đăng ký"));
        card.add(Box.createVerticalStrut(8));
        configureField(forgotEmailField, "Email khôi phục mật khẩu");
        card.add(forgotEmailField);
        card.add(forgotEmailError);
        card.add(Box.createVerticalStrut(8));

        JButton back = linkButton("Quay lại đăng nhập");
        back.setAlignmentX(LEFT_ALIGNMENT);
        back.setMaximumSize(new Dimension(180, 32));
        back.addActionListener(event -> setMode(false));
        card.add(back);
        return card;
    }

    private JPanel buildRegistrationCard() {
        JPanel card = verticalCard();
        card.add(fieldLabel("Họ và tên"));
        card.add(Box.createVerticalStrut(5));
        configureCompactField(fullNameField, "Họ và tên");
        card.add(fullNameField);
        card.add(fullNameError);

        card.add(fieldLabel("Email"));
        card.add(Box.createVerticalStrut(5));
        configureCompactField(registrationEmailField, "Email đăng ký");
        card.add(registrationEmailField);
        card.add(registrationEmailError);

        JPanel passwordLabels = new JPanel(new GridLayout(1, 2, 10, 0));
        passwordLabels.setOpaque(false);
        passwordLabels.setAlignmentX(LEFT_ALIGNMENT);
        passwordLabels.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        passwordLabels.add(fieldLabel("Mật khẩu"));
        passwordLabels.add(fieldLabel("Xác nhận mật khẩu"));
        card.add(passwordLabels);
        card.add(Box.createVerticalStrut(5));

        JPanel passwordFields = new JPanel(new GridLayout(1, 2, 10, 0));
        passwordFields.setOpaque(false);
        passwordFields.setAlignmentX(LEFT_ALIGNMENT);
        passwordFields.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        configureCompactField(registrationPasswordField, "Mật khẩu đăng ký");
        configureCompactField(confirmPasswordField, "Xác nhận mật khẩu");
        passwordFields.add(registrationPasswordField);
        passwordFields.add(confirmPasswordField);
        card.add(passwordFields);

        JPanel passwordErrors = new JPanel(new GridLayout(1, 2, 10, 0));
        passwordErrors.setOpaque(false);
        passwordErrors.setAlignmentX(LEFT_ALIGNMENT);
        passwordErrors.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        passwordErrors.add(registrationPasswordError);
        passwordErrors.add(confirmPasswordError);
        card.add(passwordErrors);
        registrationPasswordField.getDocument().addDocumentListener(
                onTextChange(this::updateRegistrationPasswordHint));

        JCheckBox showPassword = compactCheckBox("Hiện mật khẩu");
        char passwordEcho = registrationPasswordField.getEchoChar();
        showPassword.addActionListener(event -> registrationPasswordField.setEchoChar(
                showPassword.isSelected() ? (char) 0 : passwordEcho));
        JCheckBox showConfirmation = compactCheckBox("Hiện xác nhận");
        char confirmEcho = confirmPasswordField.getEchoChar();
        showConfirmation.addActionListener(event -> confirmPasswordField.setEchoChar(
                showConfirmation.isSelected() ? (char) 0 : confirmEcho));
        JPanel passwordToggles = new JPanel(new GridLayout(1, 2, 10, 0));
        passwordToggles.setOpaque(false);
        passwordToggles.setAlignmentX(LEFT_ALIGNMENT);
        passwordToggles.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        passwordToggles.add(showPassword);
        passwordToggles.add(showConfirmation);
        card.add(passwordToggles);

        JPanel signInPrompt = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 4, 0));
        signInPrompt.setOpaque(false);
        signInPrompt.setAlignmentX(LEFT_ALIGNMENT);
        signInPrompt.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel existingAccount = new JLabel("Đã có tài khoản?");
        existingAccount.setFont(BODY);
        existingAccount.setForeground(MUTED);
        signInPrompt.add(existingAccount);
        JButton signIn = linkButton("Đăng nhập");
        signIn.addActionListener(event -> setMode(false));
        signInPrompt.add(signIn);
        card.add(signInPrompt);
        return card;
    }

    private static JPanel verticalCard() {
        JPanel card = new JPanel();
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        return card;
    }

    private void configureField(JTextField field, String accessibleName) {
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        field.setForeground(INK);
        field.setCaretColor(INK);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        field.setPreferredSize(new Dimension(420, 54));
        field.setBorder(new EmptyBorder(0, 18, 0, 18));
        field.setAlignmentX(LEFT_ALIGNMENT);
        field.getAccessibleContext().setAccessibleName(accessibleName);
    }

    private void setAuthCardHeight(int height) {
        authCards.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        authCards.setPreferredSize(new Dimension(420, height));
        authCards.revalidate();
    }

    private void configureCompactField(JTextField field, String accessibleName) {
        configureField(field, accessibleName);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        field.setPreferredSize(new Dimension(420, 48));
    }

    private static JCheckBox compactCheckBox(String text) {
        JCheckBox checkBox = new JCheckBox(text);
        configureCheckBox(checkBox);
        return checkBox;
    }

    private static void configureCheckBox(JCheckBox checkBox) {
        checkBox.setOpaque(false);
        checkBox.setForeground(INK);
        checkBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        checkBox.setIcon(new CheckBoxIcon(false));
        checkBox.setSelectedIcon(new CheckBoxIcon(true));
        checkBox.setDisabledIcon(new CheckBoxIcon(false, false));
        checkBox.setDisabledSelectedIcon(new CheckBoxIcon(true, false));
        checkBox.setIconTextGap(9);
        checkBox.setFocusPainted(false);
        checkBox.setBorderPainted(false);
        checkBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        checkBox.setAlignmentX(LEFT_ALIGNMENT);
        checkBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
    }

    private void setMode(boolean signUp) {
        if (registrationPending || loginPending) {
            modeSwitch.setSignUp(signUpMode);
            return;
        }
        forgotPasswordMode = false;
        signUpMode = signUp;
        setAuthCardHeight(AUTH_CARD_DEFAULT_HEIGHT);
        setTitle(signUp ? "Java Chat - Đăng ký" : "Java Chat - Đăng nhập");
        modeSwitch.setVisible(true);
        modeSwitch.setSignUp(signUp);
        heading.setText(signUp ? "Tạo tài khoản mới" : "Chào mừng trở lại!");
        subheading.setText(signUp
                ? "Tham gia Java Chat và bắt đầu cuộc trò chuyện đầu tiên."
                : "Đăng nhập để tiếp tục trò chuyện cùng bạn bè.");
        submitButton.setText(signUp ? "Đăng ký" : "Đăng nhập");
        submitButton.setEnabled(true);
        authCardLayout.show(authCards, signUp ? "register" : "login");
        clearErrors();
        if (signUp) updateRegistrationPasswordHint();
        javax.swing.SwingUtilities.invokeLater(() ->
                (signUp ? fullNameField : loginField).requestFocusInWindow());
    }

    private void showForgotPassword() {
        if (loginPending) return;
        clearErrors();
        forgotPasswordMode = true;
        signUpMode = false;
        setAuthCardHeight(AUTH_CARD_FORGOT_HEIGHT);
        setTitle("Java Chat - Quên mật khẩu");
        modeSwitch.setVisible(false);
        heading.setText("Quên mật khẩu?");
        subheading.setText("Đừng lo, Java Chat sẽ giúp bạn quay lại.");
        submitButton.setText("Gửi mã xác minh");
        submitButton.setEnabled(true);
        authCardLayout.show(authCards, "forgot");
        String login = loginField.getText().trim();
        if (login.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            forgotEmailField.setText(login);
        }
        javax.swing.SwingUtilities.invokeLater(forgotEmailField::requestFocusInWindow);
    }

    private void handleEscape() {
        if (forgotPasswordMode) {
            setMode(false);
            return;
        }
        dispose();
    }

    private void submit() {
        if (registrationPending || loginPending) return;
        clearErrors();
        if (forgotPasswordMode) {
            submitForgotPassword();
            return;
        }
        if (signUpMode) {
            submitRegistration();
            return;
        }

        String login = loginField.getText().strip();
        String password = new String(passwordField.getPassword());
        boolean valid = true;
        if (login.isBlank()) {
            showFieldError(loginField, loginError, "Vui lòng nhập email hoặc tên đăng nhập.");
            valid = false;
        }
        if (password.isEmpty()) {
            showFieldError(passwordField, passwordError, "Vui lòng nhập mật khẩu.");
            valid = false;
        }
        if (!valid) return;

        loginPending = true;
        submitButton.setEnabled(false);
        submitButton.setText("Đang đăng nhập…");
        if (loginHandler == null) {
            dispose();
            onAuthenticated.run();
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                loginHandler.login(login, password);
            } catch (Exception error) {
                throw new CompletionException(error);
            }
        }).whenComplete((ignored, error) -> javax.swing.SwingUtilities.invokeLater(() -> {
            if (!isDisplayable()) return;
            loginPending = false;
            if (error != null) {
                submitButton.setEnabled(true);
                submitButton.setText("Đăng nhập");
                Throwable cause = rootCause(error);
                setStatus(cause instanceof ApiException apiError && apiError.statusCode() == 401
                        ? "Email, tên đăng nhập hoặc mật khẩu không đúng."
                        : "Chưa thể đăng nhập. Vui lòng thử lại.", true);
                return;
            }
            passwordField.setText("");
            dispose();
            onAuthenticated.run();
        }));
    }

    private void submitForgotPassword() {
        String email = forgotEmailField.getText().trim();
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            showFieldError(forgotEmailField, forgotEmailError, "Vui lòng nhập một địa chỉ email hợp lệ.");
            return;
        }

        if (passwordResetHandler == null) {
            setStatus("Hãy chạy chế độ live để đặt lại mật khẩu.", true);
            return;
        }
        requestPasswordReset(email, null);
    }

    private void requestPasswordReset(String email, String otp) {
        loginPending = true;
        submitButton.setEnabled(false);
        submitButton.setText("Đang gửi email…");
        CompletableFuture.runAsync(() -> {
            try {
                passwordResetHandler.request(email, otp);
            } catch (Exception error) {
                throw new CompletionException(error);
            }
        }).whenComplete((ignored, error) -> javax.swing.SwingUtilities.invokeLater(() -> {
            if (!isDisplayable()) return;
            loginPending = false;
            submitButton.setEnabled(true);
            submitButton.setText("Gửi mã xác minh");
            if (error != null) {
                Throwable cause = rootCause(error);
                if (cause instanceof ApiException api && api.statusCode() == 422) {
                    promptPasswordReset(email, "Mã OTP không đúng. ");
                } else {
                    setStatus(cause instanceof ApiException api && (api.statusCode() == 410 || api.statusCode() == 429)
                            ? api.getMessage() : "Chưa thể đặt lại mật khẩu. Vui lòng thử lại.", true);
                }
            } else if (otp == null) {
                promptPasswordReset(email, "");
            } else {
                setMode(false);
                loginField.setText(email);
                setStatus("Nếu email đã đăng ký, mật khẩu mới đã được gửi.", false);
            }
        }));
    }

    private void promptPasswordReset(String email, String message) {
        String otp = showOtpDialog("Đặt lại mật khẩu", email, message);
        if (otp != null) requestPasswordReset(email, otp);
    }

    private void submitRegistration() {
        String fullName = fullNameField.getText();
        String email = registrationEmailField.getText();
        String password = new String(registrationPasswordField.getPassword());
        String confirmation = new String(confirmPasswordField.getPassword());
        RegistrationValidator.Errors errors = RegistrationValidator.validate(
                fullName, email, password, confirmation);
        if (errors.fullNameError() != null) showFieldError(fullNameField, fullNameError, errors.fullNameError());
        if (errors.emailError() != null) showFieldError(registrationEmailField, registrationEmailError, errors.emailError());
        registrationPasswordError.setForeground(ERROR);
        if (errors.passwordError() != null) {
            showFieldError(registrationPasswordField, registrationPasswordError, errors.passwordError());
        } else {
            updateRegistrationPasswordHint();
        }
        if (errors.confirmationError() != null) showFieldError(confirmPasswordField, confirmPasswordError, errors.confirmationError());
        if (!errors.valid()) return;
        fullName = errors.fullName();
        email = errors.email();

        if (registrationHandler == null) {
            setStatus("Chế độ demo không lưu tài khoản. Hãy chạy server và client live.", true);
            return;
        }

        registrationPending = true;
        submitButton.setEnabled(false);
        submitButton.setText("Đang gửi mã OTP…");
        String submittedName = fullName;
        String submittedEmail = email;
        CompletableFuture.runAsync(() -> {
            try {
                otpSender.send(submittedEmail);
            } catch (Exception error) {
                throw new CompletionException(error);
            }
        }).whenComplete((ignored, error) -> javax.swing.SwingUtilities.invokeLater(() -> {
            if (!isDisplayable()) return;
            if (error != null) {
                finishRegistrationAttempt();
                Throwable cause = rootCause(error);
                setStatus(cause instanceof ApiException api && api.statusCode() == 429
                        ? api.getMessage() : "Chưa thể gửi mã OTP. Vui lòng thử lại.", true);
                return;
            }
            promptForOtp(submittedName, submittedEmail, password, "");
        }));
    }

    private void promptForOtp(String name, String email, String password, String errorMessage) {
        String otp = showOtpDialog("Xác minh email", email, errorMessage);
        if (otp == null) finishRegistrationAttempt();
        else verifyRegistration(name, email, password, otp);
    }

    public static void previewOtp() {
        LoginFrame owner = new LoginFrame(() -> { }, null, null, null, null);
        try {
            owner.showOtpDialog("Xác minh email", "demo@example.com", "");
        } finally {
            owner.dispose();
        }
    }

    private String showOtpDialog(String title, String email, String errorMessage) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(SURFACE);
        body.setBorder(new EmptyBorder(24, 28, 22, 28));
        dialog.setContentPane(body);

        JPanel copy = verticalCard();
        JLabel heading = new JLabel(title);
        heading.setForeground(INK);
        heading.setFont(BODY.deriveFont(Font.BOLD, 24f));
        copy.add(heading);
        copy.add(Box.createVerticalStrut(8));
        JTextArea instructions = new JTextArea("Nhập mã 6 số đã gửi đến " + email + ". Mã có hiệu lực trong 5 phút.");
        instructions.setFont(BODY);
        instructions.setForeground(MUTED);
        instructions.setOpaque(false);
        instructions.setEditable(false);
        instructions.setLineWrap(true);
        instructions.setWrapStyleWord(true);
        instructions.setColumns(29);
        instructions.setAlignmentX(LEFT_ALIGNMENT);
        copy.add(instructions);
        body.add(copy, BorderLayout.NORTH);

        JPanel input = verticalCard();
        JLabel label = fieldLabel("Mã OTP");
        JTextField code = new RoundedTextField("");
        label.setLabelFor(code);
        configureField(code, "Mã OTP gồm 6 chữ số");
        code.setHorizontalAlignment(JTextField.CENTER);
        code.setFont(new Font(Font.MONOSPACED, Font.BOLD, 26));
        input.add(label);
        input.add(Box.createVerticalStrut(6));
        input.add(code);
        JLabel feedback = errorLabel();
        feedback.setText(errorMessage.isBlank() ? " " : errorMessage.strip());
        input.add(feedback);
        body.add(input, BorderLayout.CENTER);

        RoundedButton confirm = new RoundedButton("Xác minh", true);
        confirm.setEnabled(false);
        JButton cancel = linkButton("Hủy");
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);
        confirm.setPreferredSize(new Dimension(136, 44));
        actions.add(cancel);
        actions.add(confirm);
        body.add(actions, BorderLayout.SOUTH);

        String[] result = new String[1];
        code.getDocument().addDocumentListener(onTextChange(() -> {
            String value = code.getText().strip();
            boolean invalid = !value.matches("[0-9]{0,6}");
            confirm.setEnabled(!invalid && value.length() == 6);
            feedback.setText(invalid ? "Vui lòng nhập đúng 6 chữ số." : " ");
            setInvalid(code, invalid);
        }));
        confirm.addActionListener(event -> {
            result[0] = code.getText().strip();
            dialog.dispose();
        });
        cancel.addActionListener(event -> dialog.dispose());
        dialog.getRootPane().setDefaultButton(confirm);
        dialog.getRootPane().registerKeyboardAction(event -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        javax.swing.SwingUtilities.invokeLater(code::requestFocusInWindow);
        dialog.setVisible(true);
        return result[0];
    }

    private static javax.swing.event.DocumentListener onTextChange(Runnable update) {
        return new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent event) { update.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent event) { update.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent event) { update.run(); }
        };
    }

    private void updateRegistrationPasswordHint() {
        String password = new String(registrationPasswordField.getPassword());
        String error = com.javachat.shared.model.RegistrationPassword.error(password);
        registrationPasswordError.setForeground(password.isEmpty() ? MUTED
                : error == null ? new Color(36, 118, 67) : ERROR);
        registrationPasswordError.setText(password.isEmpty() ? "8–72 ký tự, có chữ in hoa."
                : error == null ? "Mật khẩu đạt yêu cầu." : error);
        setInvalid(registrationPasswordField, !password.isEmpty() && error != null);
    }

    private void finishRegistrationAttempt() {
        registrationPending = false;
        submitButton.setEnabled(true);
        submitButton.setText("Đăng ký");
    }

    private void verifyRegistration(String name, String email, String password, String otp) {
        submitButton.setText("Đang đăng ký…");
        CompletableFuture.supplyAsync(() -> {
            try {
                return registrationHandler.register(name, email, password, otp);
            } catch (Exception error) {
                throw new CompletionException(error);
            }
        }).whenComplete((userId, error) -> javax.swing.SwingUtilities.invokeLater(() -> {
            if (!isDisplayable()) return;
            finishRegistrationAttempt();
            if (error != null) {
                Throwable cause = rootCause(error);
                if (cause instanceof ApiException apiError && apiError.statusCode() == 422) {
                    registrationPending = true;
                    submitButton.setEnabled(false);
                    promptForOtp(name, email, password, apiError.getMessage() + "\n");
                } else if (cause instanceof ApiException apiError && apiError.statusCode() == 410) {
                    setStatus("Mã OTP không còn hiệu lực. Bấm Đăng ký để gửi mã mới.", true);
                } else if (cause instanceof ApiException apiError && apiError.statusCode() == 409) {
                    showFieldError(registrationEmailField, registrationEmailError,
                            "Email này đã được đăng ký.");
                } else {
                    setStatus("Chưa thể hoàn tất đăng ký. Vui lòng thử lại.", true);
                }
                return;
            }
            registrationPasswordField.setText("");
            confirmPasswordField.setText("");
            loginField.setText(email);
            setMode(false);
            setStatus("Đăng ký thành công. Vui lòng đăng nhập để tiếp tục.", false);
        }));
    }

    private static Throwable rootCause(Throwable error) {
        Throwable result = error;
        while (result.getCause() != null) result = result.getCause();
        return result;
    }

    private void clearErrors() {
        loginError.setText(" ");
        passwordError.setText(" ");
        fullNameError.setText(" ");
        registrationEmailError.setText(" ");
        registrationPasswordError.setText(" ");
        confirmPasswordError.setText(" ");
        forgotEmailError.setText(" ");
        formStatus.setText(" ");
        setInvalid(loginField, false);
        setInvalid(passwordField, false);
        setInvalid(fullNameField, false);
        setInvalid(registrationEmailField, false);
        setInvalid(registrationPasswordField, false);
        setInvalid(confirmPasswordField, false);
        setInvalid(forgotEmailField, false);
    }

    private void showFieldError(JTextField field, JLabel label, String message) {
        label.setText(message);
        setInvalid(field, true);
        if (!field.isFocusOwner()) field.requestFocusInWindow();
    }

    private static void setInvalid(JTextField field, boolean invalid) {
        if (field instanceof RoundedTextField rounded) rounded.setInvalid(invalid);
        if (field instanceof RoundedPasswordField rounded) rounded.setInvalid(invalid);
        field.repaint();
    }

    private void setStatus(String message, boolean error) {
        formStatus.setText(message);
        formStatus.setForeground(error ? ERROR : MUTED);
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(MEDIUM);
        label.setForeground(INK);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel errorLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        label.setForeground(ERROR);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        return label;
    }

    private static JPanel divider() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        JPanel left = new JPanel();
        left.setBackground(BORDER);
        left.setPreferredSize(new Dimension(1, 1));
        row.add(left, gbc);
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 12, 0, 12);
        JLabel text = new JLabel("HOẶC TIẾP TỤC VỚI");
        text.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        text.setForeground(MUTED);
        row.add(text, gbc);
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel right = new JPanel();
        right.setBackground(BORDER);
        right.setPreferredSize(new Dimension(1, 1));
        row.add(right, gbc);
        return row;
    }

    private static JButton linkButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        button.setForeground(ORANGE_DARK);
        button.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 0));
        button.setContentAreaFilled(false);
        button.setFocusPainted(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static final class HeroPanel extends JPanel {
        private static final int FRAME_DELAY_MS = 40;
        private static final int KNOCK_CYCLE_MS = 2_800;
        private final BufferedImage[] knockFrames;
        private final Timer knockTimer;
        private int animationTime;

        HeroPanel() {
            setBackground(HERO_BG);
            try {
                BufferedImage sprite = ImageIO.read(Objects.requireNonNull(
                        LoginFrame.class.getResourceAsStream("/images/cat-knocking-sprite.png")));
                int frameWidth = sprite.getWidth() / 2;
                int frameHeight = sprite.getHeight() / 2;
                knockFrames = new BufferedImage[] {
                        sprite.getSubimage(0, 0, frameWidth, frameHeight),
                        sprite.getSubimage(frameWidth, 0, frameWidth, frameHeight),
                        sprite.getSubimage(0, frameHeight, frameWidth, frameHeight),
                        sprite.getSubimage(frameWidth, frameHeight, frameWidth, frameHeight)
                };
            } catch (IOException | NullPointerException exception) {
                throw new IllegalStateException("Không thể tải sprite mascot đăng nhập", exception);
            }
            knockTimer = new Timer(FRAME_DELAY_MS, event -> {
                animationTime = (animationTime + FRAME_DELAY_MS) % KNOCK_CYCLE_MS;
                repaint();
            });
            knockTimer.setCoalesce(true);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            animationTime = 0;
            knockTimer.start();
        }

        @Override
        public void removeNotify() {
            knockTimer.stop();
            super.removeNotify();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            BufferedImage mascot = knockFrames[frameAt(animationTime)];
            int maxWidth = Math.max(260, getWidth() - 64);
            int maxHeight = Math.max(330, getHeight() - 92);
            double scale = Math.min((double) maxWidth / mascot.getWidth(null),
                    (double) maxHeight / mascot.getHeight(null));
            int width = (int) (mascot.getWidth(null) * scale);
            int height = (int) (mascot.getHeight(null) * scale);
            int x = (getWidth() - width) / 2;
            int y = Math.max(35, (getHeight() - height) / 2);
            g.drawImage(mascot, x, y, width, height, null);
            g.dispose();
        }

        private static int frameAt(int elapsed) {
            if (elapsed < 140) return 0;
            if (elapsed < 260) return 1;
            if (elapsed < 390) return 2;
            if (elapsed < 520) return 3;
            if (elapsed < 700) return 0;
            if (elapsed < 820) return 1;
            if (elapsed < 950) return 2;
            if (elapsed < 1_080) return 3;
            return 0;
        }
    }

    private static final class PawModeSwitch extends JPanel {
        private static final Color TRACK = new Color(244, 216, 193);
        private static final Color ACTIVE = new Color(238, 161, 99);
        private static final int PAW_SIZE = 30;
        private final BufferedImage paw;
        private final Timer snapTimer;
        private java.util.function.Consumer<Boolean> selectionListener = ignored -> { };
        private double position;
        private double targetPosition;
        private boolean signUp;
        private boolean dragging;

        PawModeSwitch() {
            setOpaque(false);
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleName("Chuyển giữa đăng nhập và đăng ký");
            getAccessibleContext().setAccessibleDescription(
                    "Kéo bàn chân mèo sang trái để đăng nhập, sang phải để đăng ký");
            try {
                paw = ImageIO.read(Objects.requireNonNull(
                        LoginFrame.class.getResourceAsStream("/images/cat-paw-handle.png")));
            } catch (IOException | NullPointerException exception) {
                throw new IllegalStateException("Không thể tải tay nắm bàn chân mèo", exception);
            }

            snapTimer = new Timer(16, event -> {
                double distance = targetPosition - position;
                if (Math.abs(distance) < 0.01) {
                    position = targetPosition;
                    ((Timer) event.getSource()).stop();
                } else {
                    position += distance * 0.24;
                }
                repaint();
            });
            snapTimer.setCoalesce(true);

            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    requestFocusInWindow();
                    dragging = isOnPaw(event.getX(), event.getY());
                    if (!dragging) select(event.getX() >= getWidth() / 2, true);
                }

                @Override
                public void mouseDragged(MouseEvent event) {
                    if (!dragging) return;
                    snapTimer.stop();
                    int left = knobCenter(false);
                    int right = knobCenter(true);
                    position = Math.max(0, Math.min(1, (event.getX() - left) / (double) (right - left)));
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    if (!dragging) return;
                    dragging = false;
                    select(position >= 0.5, true);
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);

            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "signIn");
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "signUp");
            getActionMap().put("signIn", new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent event) { select(false, true); }
            });
            getActionMap().put("signUp", new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent event) { select(true, true); }
            });
        }

        void setSelectionListener(java.util.function.Consumer<Boolean> listener) {
            selectionListener = listener == null ? ignored -> { } : listener;
        }

        void setSignUp(boolean value) {
            select(value, false);
        }

        private void select(boolean value, boolean notify) {
            boolean changed = signUp != value;
            signUp = value;
            targetPosition = value ? 1 : 0;
            if (!dragging) snapTimer.restart();
            getAccessibleContext().setAccessibleDescription(value
                    ? "Đăng ký đang được chọn" : "Đăng nhập đang được chọn");
            if (notify && changed) selectionListener.accept(value);
        }

        private int knobCenter(boolean right) {
            return Math.round(getWidth() * (right ? 0.75f : 0.25f));
        }

        private boolean isOnPaw(int mouseX, int mouseY) {
            int centerX = (int) Math.round(knobCenter(false)
                    + (knobCenter(true) - knobCenter(false)) * position);
            int centerY = 16;
            return Math.abs(mouseX - centerX) <= PAW_SIZE
                    && Math.abs(mouseY - centerY) <= PAW_SIZE;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            int width = getWidth();
            int height = getHeight();
            g.setColor(TRACK);
            g.fillRoundRect(0, 0, width, height, height, height);

            int inset = 5;
            int segmentWidth = (width - inset * 2) / 2;
            int activeX = inset + (int) Math.round(segmentWidth * position);
            g.setColor(ACTIVE);
            g.fillRoundRect(activeX, inset, segmentWidth, height - inset * 2,
                    height - inset * 2, height - inset * 2);

            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            int baseline = height - 11;
            drawCentered(g, "Đăng nhập", width / 4, baseline, signUp ? new Color(108, 75, 55) : INK);
            drawCentered(g, "Đăng ký", width * 3 / 4, baseline, signUp ? INK : new Color(108, 75, 55));

            int left = knobCenter(false);
            int right = knobCenter(true);
            int pawX = (int) Math.round(left + (right - left) * position) - PAW_SIZE / 2;
            int pawY = 1;
            g.drawImage(paw, pawX, pawY, PAW_SIZE, PAW_SIZE, null);

            if (isFocusOwner()) {
                g.setColor(FOCUS);
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(2, 2, width - 5, height - 5, height - 4, height - 4);
            }
            g.dispose();
        }

        private static void drawCentered(Graphics2D g, String text, int centerX, int baseline, Color color) {
            g.setColor(color);
            g.drawString(text, centerX - g.getFontMetrics().stringWidth(text) / 2, baseline);
        }
    }

    private static final class RoundedButton extends JButton {
        private final boolean primary;
        private boolean hover;

        RoundedButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            setForeground(primary ? Color.WHITE : INK);
            setBackground(primary ? ORANGE : SURFACE);
            setBorder(new EmptyBorder(0, 22, 0, 22));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(420, 54));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent event) { hover = true; repaint(); }
                @Override public void mouseExited(java.awt.event.MouseEvent event) { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = primary ? (hover ? ORANGE_DARK : ORANGE) : (hover ? FIELD_BG : SURFACE);
            if (!isEnabled()) fill = new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 145);
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
            if (!primary) {
                g.setColor(BORDER);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
            }
            if (isFocusOwner()) {
                g.setColor(FOCUS);
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 25, 25);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private abstract static class RoundedFieldSupport {
        static void install(JTextField field) {
            field.setOpaque(false);
            field.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent event) { field.repaint(); }
                @Override public void focusLost(FocusEvent event) { field.repaint(); }
            });
        }

        static void paintBackground(JTextField field, Graphics graphics, boolean invalid) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(FIELD_BG);
            g.fillRoundRect(0, 0, field.getWidth(), field.getHeight(), 24, 24);
            g.setColor(invalid ? ERROR : field.isFocusOwner() ? FOCUS : BORDER);
            g.setStroke(new BasicStroke(field.isFocusOwner() || invalid ? 2 : 1));
            g.drawRoundRect(1, 1, field.getWidth() - 3, field.getHeight() - 3, 24, 24);
            g.dispose();
        }
    }

    private static final class RoundedTextField extends JTextField {
        private final String placeholder;
        private boolean invalid;

        RoundedTextField(String placeholder) {
            this.placeholder = placeholder;
            RoundedFieldSupport.install(this);
        }

        void setInvalid(boolean invalid) { this.invalid = invalid; }

        @Override protected void paintComponent(Graphics graphics) {
            RoundedFieldSupport.paintBackground(this, graphics, invalid);
            super.paintComponent(graphics);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setColor(MUTED);
                g.setFont(getFont());
                g.drawString(placeholder, getInsets().left, (getHeight() + g.getFontMetrics().getAscent()) / 2 - 2);
                g.dispose();
            }
        }
    }

    private static final class RoundedPasswordField extends JPasswordField {
        private boolean invalid;

        RoundedPasswordField() {
            RoundedFieldSupport.install(this);
        }

        void setInvalid(boolean invalid) { this.invalid = invalid; }

        @Override protected void paintComponent(Graphics graphics) {
            RoundedFieldSupport.paintBackground(this, graphics, invalid);
            super.paintComponent(graphics);
        }
    }

    private static final class CheckBoxIcon implements javax.swing.Icon {
        private static final int SIZE = 19;
        private final boolean selected;
        private final boolean enabled;

        CheckBoxIcon(boolean selected) {
            this(selected, true);
        }

        CheckBoxIcon(boolean selected, boolean enabled) {
            this.selected = selected;
            this.enabled = enabled;
        }

        @Override public int getIconWidth() { return SIZE; }
        @Override public int getIconHeight() { return SIZE; }

        @Override
        public void paintIcon(java.awt.Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = selected ? ORANGE : SURFACE;
            Color outline = selected ? ORANGE : BORDER;
            if (!enabled) {
                fill = new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 110);
                outline = new Color(outline.getRed(), outline.getGreen(), outline.getBlue(), 110);
            }
            g.setColor(fill);
            g.fillRoundRect(x, y, SIZE - 1, SIZE - 1, 6, 6);
            g.setColor(outline);
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(x + 1, y + 1, SIZE - 3, SIZE - 3, 5, 5);
            if (selected) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(x + 5, y + 9, x + 8, y + 12);
                g.drawLine(x + 8, y + 12, x + 14, y + 6);
            }
            g.dispose();
        }
    }

    private static final class GoogleIcon implements javax.swing.Icon {
        @Override public int getIconWidth() { return 24; }
        @Override public int getIconHeight() { return 24; }

        @Override
        public void paintIcon(java.awt.Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(66, 133, 244));
            g.draw(new Arc2D.Double(x + 2, y + 2, 19, 19, -45, 95, Arc2D.OPEN));
            g.setColor(new Color(52, 168, 83));
            g.draw(new Arc2D.Double(x + 2, y + 2, 19, 19, 50, 78, Arc2D.OPEN));
            g.setColor(new Color(251, 188, 5));
            g.draw(new Arc2D.Double(x + 2, y + 2, 19, 19, 128, 70, Arc2D.OPEN));
            g.setColor(new Color(234, 67, 53));
            g.draw(new Arc2D.Double(x + 2, y + 2, 19, 19, 198, 117, Arc2D.OPEN));
            g.setColor(new Color(66, 133, 244));
            g.setStroke(new BasicStroke(4));
            g.drawLine(x + 13, y + 12, x + 22, y + 12);
            g.dispose();
        }
    }
}
