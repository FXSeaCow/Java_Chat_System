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

    private final Runnable onAuthenticated;
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
    private final JLabel termsError = errorLabel();
    private final JCheckBox termsCheckBox = new JCheckBox("Tôi đồng ý với Điều khoản sử dụng");
    private final CardLayout authCardLayout = new CardLayout();
    private final JPanel authCards = new JPanel(authCardLayout);
    private final JLabel formStatus = new JLabel(" ");
    private final RoundedButton submitButton = new RoundedButton("Đăng nhập", true);
    private final PawModeSwitch modeSwitch = new PawModeSwitch();
    private final JLabel heading = new JLabel("Chào mừng trở lại!");
    private final JLabel subheading = new JLabel("Đăng nhập để tiếp tục trò chuyện cùng bạn bè.");
    private boolean signUpMode;

    public LoginFrame(Runnable onAuthenticated) {
        super("Java Chat - Đăng nhập");
        this.onAuthenticated = Objects.requireNonNull(onAuthenticated);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Rectangle usableScreen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int windowWidth = Math.min(1_040, Math.max(840, (int) (usableScreen.width * 0.88)));
        int windowHeight = Math.min(650, Math.max(560, (int) (usableScreen.height * 0.88)));
        setSize(windowWidth, windowHeight);
        setMinimumSize(new Dimension(840, 560));
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        getRootPane().setDefaultButton(submitButton);
        getRootPane().registerKeyboardAction(event -> dispose(),
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
        authCards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 330));
        authCards.setPreferredSize(new Dimension(420, 330));
        authCards.add(buildLoginCard(), "login");
        authCards.add(buildRegistrationCard(), "register");
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

        JPanel passwordRow = new JPanel(new BorderLayout());
        passwordRow.setOpaque(false);
        passwordRow.setAlignmentX(LEFT_ALIGNMENT);
        passwordRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        passwordRow.add(fieldLabel("Mật khẩu"), BorderLayout.WEST);
        JButton forgot = linkButton("Quên mật khẩu?");
        forgot.addActionListener(event -> setStatus("Liên kết đặt lại mật khẩu sẽ được gửi qua email.", false));
        passwordRow.add(forgot, BorderLayout.EAST);
        card.add(passwordRow);
        card.add(Box.createVerticalStrut(5));
        configureField(passwordField, "Mật khẩu");
        card.add(passwordField);
        card.add(passwordError);

        JCheckBox showPassword = compactCheckBox("Hiện mật khẩu");
        char echo = passwordField.getEchoChar();
        showPassword.addActionListener(event -> passwordField.setEchoChar(
                showPassword.isSelected() ? (char) 0 : echo));
        card.add(showPassword);
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

        JPanel labels = new JPanel(new GridLayout(1, 2, 10, 0));
        labels.setOpaque(false);
        labels.setAlignmentX(LEFT_ALIGNMENT);
        labels.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        labels.add(fieldLabel("Mật khẩu"));
        labels.add(fieldLabel("Xác nhận mật khẩu"));
        card.add(labels);
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

        JCheckBox showPassword = compactCheckBox("Hiện mật khẩu");
        char passwordEcho = registrationPasswordField.getEchoChar();
        char confirmEcho = confirmPasswordField.getEchoChar();
        showPassword.addActionListener(event -> {
            registrationPasswordField.setEchoChar(showPassword.isSelected() ? (char) 0 : passwordEcho);
            confirmPasswordField.setEchoChar(showPassword.isSelected() ? (char) 0 : confirmEcho);
        });
        card.add(showPassword);

        configureCheckBox(termsCheckBox);
        card.add(termsCheckBox);
        card.add(termsError);
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
        field.getAccessibleContext().setAccessibleName(accessibleName);
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
        checkBox.setForeground(MUTED);
        checkBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        checkBox.setFocusPainted(true);
        checkBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        checkBox.setAlignmentX(LEFT_ALIGNMENT);
        checkBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
    }

    private void setMode(boolean signUp) {
        signUpMode = signUp;
        modeSwitch.setSignUp(signUp);
        heading.setText(signUp ? "Tạo tài khoản mới" : "Chào mừng trở lại!");
        subheading.setText(signUp
                ? "Tham gia Java Chat và bắt đầu cuộc trò chuyện đầu tiên."
                : "Đăng nhập để tiếp tục trò chuyện cùng bạn bè.");
        submitButton.setText(signUp ? "Tạo tài khoản" : "Đăng nhập");
        authCardLayout.show(authCards, signUp ? "register" : "login");
        clearErrors();
        javax.swing.SwingUtilities.invokeLater(() ->
                (signUp ? fullNameField : loginField).requestFocusInWindow());
    }

    private void submit() {
        clearErrors();
        if (signUpMode) {
            submitRegistration();
            return;
        }

        String login = loginField.getText().trim();
        String password = new String(passwordField.getPassword());
        boolean valid = true;
        if (login.isBlank()) {
            showFieldError(loginField, loginError, "Vui lòng nhập email hoặc tên đăng nhập.");
            valid = false;
        }
        if (password.length() < 6) {
            showFieldError(passwordField, passwordError, "Mật khẩu cần có ít nhất 6 ký tự.");
            valid = false;
        }
        if (!valid) return;

        submitButton.setEnabled(false);
        submitButton.setText(signUpMode ? "Đang tạo tài khoản..." : "Đang đăng nhập...");
        setStatus("Đang kết nối an toàn...", false);
        Timer timer = new Timer(450, event -> {
            ((Timer) event.getSource()).stop();
            dispose();
            onAuthenticated.run();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void submitRegistration() {
        String fullName = fullNameField.getText().trim();
        String email = registrationEmailField.getText().trim();
        String password = new String(registrationPasswordField.getPassword());
        String confirmation = new String(confirmPasswordField.getPassword());
        boolean valid = true;

        if (fullName.length() < 2) {
            showFieldError(fullNameField, fullNameError, "Vui lòng nhập họ tên.");
            valid = false;
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            showFieldError(registrationEmailField, registrationEmailError, "Email chưa hợp lệ.");
            valid = false;
        }
        if (password.length() < 8) {
            showFieldError(registrationPasswordField, registrationPasswordError, "Tối thiểu 8 ký tự.");
            valid = false;
        }
        if (!confirmation.equals(password)) {
            showFieldError(confirmPasswordField, confirmPasswordError, "Mật khẩu chưa khớp.");
            valid = false;
        }
        if (!termsCheckBox.isSelected()) {
            termsError.setText("Bạn cần đồng ý với điều khoản để tiếp tục.");
            valid = false;
        }
        if (!valid) return;

        submitButton.setEnabled(false);
        submitButton.setText("Đang tạo tài khoản...");
        setStatus("Đang thiết lập tài khoản an toàn...", false);
        Timer timer = new Timer(500, event -> {
            ((Timer) event.getSource()).stop();
            dispose();
            onAuthenticated.run();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void clearErrors() {
        loginError.setText(" ");
        passwordError.setText(" ");
        fullNameError.setText(" ");
        registrationEmailError.setText(" ");
        registrationPasswordError.setText(" ");
        confirmPasswordError.setText(" ");
        termsError.setText(" ");
        formStatus.setText(" ");
        setInvalid(loginField, false);
        setInvalid(passwordField, false);
        setInvalid(fullNameField, false);
        setInvalid(registrationEmailField, false);
        setInvalid(registrationPasswordField, false);
        setInvalid(confirmPasswordField, false);
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
