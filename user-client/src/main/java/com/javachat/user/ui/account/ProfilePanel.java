package com.javachat.user.ui.account;

import com.javachat.shared.dto.AuthDtos.ProfileUpdateRequest;
import com.javachat.shared.dto.AuthDtos.ChangePasswordRequest;
import com.javachat.shared.model.Gender;
import com.javachat.shared.model.User;
import com.javachat.user.ui.chat.ChatTheme;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Account profile form for JC-22. It edits the fields of {@link ProfileUpdateRequest}
 * and submits them through a callback; it performs no network work except loading
 * the avatar preview image.
 */
public final class ProfilePanel extends JPanel {
    private static final Color INK = ChatTheme.INK;
    private static final Color MUTED = ChatTheme.MUTED;
    private static final Color BORDER = ChatTheme.BORDER;
    private static final Color ERROR = new Color(190, 55, 55);
    private static final Color SUCCESS = ChatTheme.TEAL_DARK;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final int MAX_NAME = 100;
    private static final int MAX_ADDRESS = 255;
    private static final int MAX_URL = 2048;

    private final AvatarPreview avatar = new AvatarPreview();
    private final JLabel headline = new JLabel(" ");
    private final JLabel username = new JLabel(" ");
    private final JLabel email = new JLabel(" ");
    private final JTextField displayName = field();
    private final JTextField avatarUrl = field();
    private final JTextField address = field();
    private final JTextField dateOfBirth = field();
    private final JComboBox<GenderChoice> gender = new JComboBox<>(GenderChoice.values());
    private final JLabel nameError = errorLabel();
    private final JLabel avatarError = errorLabel();
    private final JLabel addressError = errorLabel();
    private final JLabel dateError = errorLabel();
    private final JLabel status = new JLabel(" ");
    private final JButton reset = secondary("Hoàn tác");
    private final JButton save = primary("Lưu thay đổi");
    private final JButton changePassword = secondary("Đổi mật khẩu");
    private final JButton logout = new AccountButton("Đăng xuất", AccountButton.Style.DANGER);
    private Consumer<ProfileUpdateRequest> updateListener = ignored -> { };
    private Consumer<ChangePasswordRequest> passwordChangeListener = ignored -> { };
    private Runnable logoutListener = () -> { };
    private PasswordChangeDialog passwordDialog;
    private User user;
    private boolean saving;
    private boolean loggingOut;

    public ProfilePanel() {
        super(new BorderLayout());
        setBackground(ChatTheme.MILK);
        setBorder(BorderFactory.createEmptyBorder(30, 40, 26, 40));
        add(header(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(28, 0));
        body.setOpaque(false);
        body.add(identityCard(), BorderLayout.WEST);
        body.add(form(), BorderLayout.CENTER);
        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder(22, 0, 0, 0));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);

        DocumentListener changes = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { onEdited(); }
            @Override public void removeUpdate(DocumentEvent event) { onEdited(); }
            @Override public void changedUpdate(DocumentEvent event) { onEdited(); }
        };
        for (JTextField field : new JTextField[] {displayName, avatarUrl, address, dateOfBirth}) {
            field.getDocument().addDocumentListener(changes);
            field.addActionListener(event -> submit());
        }
        gender.addActionListener(event -> onEdited());
        avatarUrl.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent event) { refreshAvatar(); }
        });
        reset.addActionListener(event -> fill(user));
        save.addActionListener(event -> submit());
        changePassword.addActionListener(event -> showPasswordDialog());
        logout.addActionListener(event -> logoutListener.run());
        updateButtons();
    }

    /**
     * Show the signed-in user's profile. Unsaved edits are kept when the same user is
     * refreshed in the background, so a snapshot reload does not wipe the form.
     */
    public void setUser(User value) {
        boolean sameUser = user != null && value != null && user.id() == value.id();
        boolean keepEdits = sameUser && isDirty();
        user = value;
        if (keepEdits) {
            showIdentity(value);
            updateButtons();
        } else {
            fill(value);
        }
    }

    public void setUpdateListener(Consumer<ProfileUpdateRequest> listener) {
        updateListener = listener == null ? ignored -> { } : listener;
    }

    public void setLogoutListener(Runnable listener) {
        logoutListener = listener == null ? () -> { } : listener;
    }

    public void setPasswordChangeListener(Consumer<ChangePasswordRequest> listener) {
        passwordChangeListener = listener == null ? ignored -> { } : listener;
    }

    public void setPasswordChangeFailed(String message) {
        if (passwordDialog != null && passwordDialog.isDisplayable()) passwordDialog.showError(message);
    }

    public void setPasswordChangeSucceeded() {
        if (passwordDialog != null) passwordDialog.dispose();
    }

    public void setLogoutPending(boolean pending) {
        loggingOut = pending;
        logout.setEnabled(!pending);
        changePassword.setEnabled(!pending && user != null);
        logout.setText(pending ? "Đang đăng xuất..." : "Đăng xuất");
    }

    private void showPasswordDialog() {
        if (user == null || loggingOut) return;
        passwordDialog = new PasswordChangeDialog(SwingUtilities.getWindowAncestor(this),
                passwordChangeListener);
        passwordDialog.setVisible(true);
        passwordDialog = null;
    }

    /** Call after the server accepted the update; the returned user becomes the new baseline. */
    public void setSaveSucceeded(User updated) {
        saving = false;
        user = updated;
        fill(updated);
        setStatus("Đã lưu thông tin tài khoản.", false);
    }

    /** Call when the update failed so the user can correct the data and retry. */
    public void setSaveFailed(String message) {
        saving = false;
        setStatus(message == null || message.isBlank() ? "Không lưu được thay đổi." : message, true);
        updateButtons();
    }

    private JPanel header() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Thông tin tài khoản");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        title.setForeground(INK);
        JLabel hint = new JLabel("Cập nhật họ tên, ảnh đại diện và thông tin cá nhân của bạn.");
        hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        hint.setForeground(MUTED);
        panel.add(title);
        panel.add(Box.createVerticalStrut(6));
        panel.add(hint);
        return panel;
    }

    private JPanel identityCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(ChatTheme.TEAL_PALE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(26, 24, 26, 24)));
        card.setPreferredSize(new Dimension(230, 0));
        avatar.setAlignmentX(CENTER_ALIGNMENT);
        headline.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        headline.setForeground(INK);
        username.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        username.setForeground(MUTED);
        email.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        email.setForeground(MUTED);
        for (JComponent label : new JComponent[] {headline, username, email}) label.setAlignmentX(CENTER_ALIGNMENT);
        card.add(avatar);
        card.add(Box.createVerticalStrut(16));
        card.add(headline);
        card.add(Box.createVerticalStrut(6));
        card.add(username);
        card.add(Box.createVerticalStrut(3));
        card.add(email);
        card.add(Box.createVerticalGlue());
        JLabel note = new JLabel("<html><div style='text-align:center;width:160px'>"
                + "Tên đăng nhập và email không thể thay đổi tại đây.</div></html>");
        note.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        note.setForeground(MUTED);
        note.setAlignmentX(CENTER_ALIGNMENT);
        card.add(Box.createVerticalStrut(18));
        card.add(note);
        return card;
    }

    private JPanel form() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        int row = 0;
        row = addField(form, c, row, "Họ tên *", displayName, nameError);
        row = addField(form, c, row, "Ảnh đại diện (URL)", avatarUrl, avatarError);
        row = addField(form, c, row, "Địa chỉ", address, addressError);

        JPanel pair = new JPanel(new GridLayout(1, 2, 20, 0));
        pair.setOpaque(false);
        pair.add(column("Ngày sinh (dd/MM/yyyy)", dateOfBirth, dateError));
        gender.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        gender.setBackground(Color.WHITE);
        gender.setPreferredSize(new Dimension(160, displayName.getPreferredSize().height));
        pair.add(column("Giới tính", gender, errorLabel()));
        c.gridy = row++;
        form.add(pair, c);

        c.gridy = row;
        c.weighty = 1;
        form.add(Box.createGlue(), c);
        return form;
    }

    private static int addField(JPanel form, GridBagConstraints c, int row, String label,
                                JComponent input, JLabel error) {
        c.gridy = row;
        form.add(column(label, input, error), c);
        return row + 1;
    }

    private static JPanel column(String text, JComponent input, JLabel error) {
        JPanel column = new JPanel(new BorderLayout(0, 6));
        column.setOpaque(false);
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        label.setForeground(INK);
        label.setLabelFor(input);
        input.getAccessibleContext().setAccessibleName(text);
        column.add(label, BorderLayout.NORTH);
        column.add(input, BorderLayout.CENTER);
        error.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        column.add(error, BorderLayout.SOUTH);
        return column;
    }

    private JPanel footer() {
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        status.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        footer.add(status, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(changePassword);
        buttons.add(logout);
        buttons.add(reset);
        buttons.add(save);
        footer.add(buttons, BorderLayout.CENTER);
        return footer;
    }

    private void fill(User value) {
        clearErrors();
        displayName.setText(value == null ? "" : nullToEmpty(value.displayName()));
        avatarUrl.setText(value == null ? "" : nullToEmpty(value.avatarUrl()));
        address.setText(value == null ? "" : nullToEmpty(value.address()));
        dateOfBirth.setText(value == null || value.dateOfBirth() == null ? "" : DATE.format(value.dateOfBirth()));
        gender.setSelectedItem(GenderChoice.of(value == null ? null : value.gender()));
        showIdentity(value);
        refreshAvatar();
        status.setText(" ");
        updateButtons();
    }

    private void showIdentity(User value) {
        headline.setText(value == null ? "Chưa đăng nhập" : label(value));
        username.setText(value == null ? " " : "@" + value.username());
        email.setText(value == null || value.email() == null ? " " : value.email());
    }

    private void onEdited() {
        if (!saving) status.setText(" ");
        avatar.setInitials(initials(displayName.getText()));
        updateButtons();
    }

    private void updateButtons() {
        boolean enabled = user != null && !saving;
        for (JComponent input : new JComponent[] {displayName, avatarUrl, address, dateOfBirth, gender}) {
            input.setEnabled(enabled);
        }
        boolean dirty = enabled && isDirty();
        save.setEnabled(dirty);
        reset.setEnabled(dirty);
        changePassword.setEnabled(user != null && !loggingOut);
        save.setText(saving ? "Đang lưu..." : "Lưu thay đổi");
    }

    private boolean isDirty() {
        if (user == null) return false;
        return !Objects.equals(displayName.getText().strip(), nullToEmpty(user.displayName()))
                || !Objects.equals(avatarUrl.getText().strip(), nullToEmpty(user.avatarUrl()))
                || !Objects.equals(address.getText().strip(), nullToEmpty(user.address()))
                || !Objects.equals(dateOfBirth.getText().strip(),
                        user.dateOfBirth() == null ? "" : DATE.format(user.dateOfBirth()))
                || ((GenderChoice) gender.getSelectedItem()).gender != user.gender();
    }

    private void submit() {
        if (user == null || saving || !isDirty()) return;
        clearErrors();
        String name = displayName.getText().strip();
        String url = avatarUrl.getText().strip();
        String place = address.getText().strip();
        String birth = dateOfBirth.getText().strip();
        JTextField firstInvalid = null;

        if (name.isEmpty()) {
            firstInvalid = invalid(displayName, nameError, "Vui lòng nhập họ tên.", firstInvalid);
        } else if (name.length() > MAX_NAME) {
            firstInvalid = invalid(displayName, nameError, "Họ tên tối đa " + MAX_NAME + " ký tự.", firstInvalid);
        }
        if (!url.isEmpty() && (url.length() > MAX_URL || !isHttpUrl(url))) {
            firstInvalid = invalid(avatarUrl, avatarError,
                    "Đường dẫn ảnh phải bắt đầu bằng http:// hoặc https://.", firstInvalid);
        }
        if (place.length() > MAX_ADDRESS) {
            firstInvalid = invalid(address, addressError, "Địa chỉ tối đa " + MAX_ADDRESS + " ký tự.", firstInvalid);
        }
        LocalDate birthDate = null;
        if (!birth.isEmpty()) {
            try {
                birthDate = LocalDate.parse(birth, DATE);
                if (!birthDate.isBefore(LocalDate.now())) {
                    firstInvalid = invalid(dateOfBirth, dateError, "Ngày sinh phải trước hôm nay.", firstInvalid);
                } else if (birthDate.isBefore(LocalDate.of(1900, 1, 1))) {
                    firstInvalid = invalid(dateOfBirth, dateError, "Ngày sinh không hợp lệ.", firstInvalid);
                }
            } catch (DateTimeParseException error) {
                firstInvalid = invalid(dateOfBirth, dateError, "Dạng dd/MM/yyyy, ví dụ 12/03/2004.", firstInvalid);
            }
        }
        if (firstInvalid != null) {
            firstInvalid.requestFocusInWindow();
            setStatus("Vui lòng kiểm tra lại các trường được đánh dấu.", true);
            return;
        }

        saving = true;
        setStatus("Đang lưu thay đổi...", false);
        updateButtons();
        updateListener.accept(new ProfileUpdateRequest(name, emptyToNull(url), emptyToNull(place), birthDate,
                ((GenderChoice) gender.getSelectedItem()).gender));
    }

    private JTextField invalid(JTextField field, JLabel error, String message, JTextField first) {
        error.setText(message);
        field.setBorder(fieldBorder(ERROR));
        return first == null ? field : first;
    }

    private void clearErrors() {
        for (JLabel label : new JLabel[] {nameError, avatarError, addressError, dateError}) label.setText(" ");
        for (JTextField field : new JTextField[] {displayName, avatarUrl, address, dateOfBirth}) {
            field.setBorder(fieldBorder(BORDER));
        }
    }

    private void setStatus(String message, boolean error) {
        status.setForeground(error ? ERROR : SUCCESS);
        status.setText(message);
    }

    private void refreshAvatar() {
        avatar.setInitials(initials(displayName.getText()));
        String url = avatarUrl.getText().strip();
        avatar.setImage(null, url);
        if (url.isEmpty() || !isHttpUrl(url)) return;
        CompletableFuture.supplyAsync(() -> loadImage(url)).thenAccept(image -> SwingUtilities.invokeLater(() -> {
            if (url.equals(avatarUrl.getText().strip())) avatar.setImage(image, url);
        }));
    }

    private static BufferedImage loadImage(String url) {
        try {
            URLConnection connection = URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(4000);
            connection.setRequestProperty("User-Agent", "E-Chat-Desktop");
            try (var input = connection.getInputStream()) {
                return ImageIO.read(input);
            }
        } catch (Exception error) {
            return null;
        }
    }

    private static boolean isHttpUrl(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) && uri.getHost() != null;
        } catch (Exception error) {
            return false;
        }
    }

    private static String label(User value) {
        return value.displayName() == null || value.displayName().isBlank() ? value.username() : value.displayName();
    }

    private static String initials(String name) {
        String[] parts = name == null ? new String[0] : name.strip().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return "?";
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    private static JTextField field() {
        JTextField field = new JTextField();
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        field.setBorder(fieldBorder(BORDER));
        return field;
    }

    private static javax.swing.border.Border fieldBorder(Color color) {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color),
                BorderFactory.createEmptyBorder(9, 12, 9, 12));
    }

    private static JLabel errorLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        label.setForeground(ERROR);
        return label;
    }

    private static JButton primary(String text) {
        return new AccountButton(text, AccountButton.Style.PRIMARY);
    }

    private static JButton secondary(String text) {
        return new AccountButton(text, AccountButton.Style.SECONDARY);
    }

    private enum GenderChoice {
        NONE("Chưa chọn", null),
        MALE("Nam", Gender.MALE),
        FEMALE("Nữ", Gender.FEMALE),
        OTHER("Khác", Gender.OTHER);

        private final String label;
        private final Gender gender;

        GenderChoice(String label, Gender gender) {
            this.label = label;
            this.gender = gender;
        }

        static GenderChoice of(Gender gender) {
            for (GenderChoice choice : values()) if (choice.gender == gender) return choice;
            return NONE;
        }

        @Override public String toString() { return label; }
    }

    /** Round avatar showing the loaded image, or the user's initials while none is available. */
    private static final class AvatarPreview extends JPanel {
        private static final int SIZE = 112;
        private Image image;
        private String initials = "?";

        AvatarPreview() {
            setOpaque(false);
            Dimension size = new Dimension(SIZE, SIZE);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            getAccessibleContext().setAccessibleName("Ảnh đại diện");
        }

        void setImage(Image value, String source) {
            image = value;
            setToolTipText(value == null && source != null && !source.isBlank()
                    ? "Chưa tải được ảnh từ đường dẫn này" : null);
            repaint();
        }

        void setInitials(String value) {
            initials = value;
            if (image == null) repaint();
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            Ellipse2D circle = new Ellipse2D.Double(2, 2, SIZE - 4, SIZE - 4);
            if (image != null) {
                g.setClip(circle);
                int w = image.getWidth(null);
                int h = image.getHeight(null);
                double scale = Math.max((SIZE - 4.0) / w, (SIZE - 4.0) / h);
                int dw = (int) Math.round(w * scale);
                int dh = (int) Math.round(h * scale);
                g.drawImage(image, 2 + (SIZE - 4 - dw) / 2, 2 + (SIZE - 4 - dh) / 2, dw, dh, null);
                g.setClip(null);
            } else {
                g.setPaint(new java.awt.GradientPaint(0, 0, new Color(73, 201, 182), SIZE, SIZE,
                        ChatTheme.TEAL_DARK));
                g.fill(circle);
                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 38));
                FontMetrics metrics = g.getFontMetrics();
                g.drawString(initials, (SIZE - metrics.stringWidth(initials)) / 2,
                        (SIZE - metrics.getHeight()) / 2 + metrics.getAscent());
            }
            g.setColor(Color.WHITE);
            g.setStroke(new java.awt.BasicStroke(3f));
            g.draw(circle);
            g.dispose();
        }
    }
}
