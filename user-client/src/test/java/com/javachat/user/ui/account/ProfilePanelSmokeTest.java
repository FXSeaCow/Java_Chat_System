package com.javachat.user.ui.account;

import com.javachat.shared.dto.AuthDtos.ProfileUpdateRequest;
import com.javachat.shared.model.Gender;
import com.javachat.shared.model.User;
import com.javachat.user.mock.MockChatStore;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Run headless: java -Djava.awt.headless=true ... ProfilePanelSmokeTest */
public final class ProfilePanelSmokeTest {
    private ProfilePanelSmokeTest() { }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(ProfilePanelSmokeTest::run);
        System.out.println("ProfilePanelSmokeTest passed");
    }

    private static void run() {
        MockChatStore store = new MockChatStore();
        User me = store.currentUser();
        ProfilePanel panel = new ProfilePanel();
        List<ProfileUpdateRequest> sent = new ArrayList<>();
        panel.setUpdateListener(sent::add);
        panel.setUser(me);

        List<JTextField> fields = find(panel, JTextField.class);
        JTextField name = fields.get(0);
        JTextField avatar = fields.get(1);
        JTextField address = fields.get(2);
        JTextField birth = fields.get(3);
        @SuppressWarnings("unchecked")
        JComboBox<Object> gender = find(panel, JComboBox.class).get(0);
        JButton save = button(panel, "Lưu thay đổi");
        JButton reset = button(panel, "Hoàn tác");

        check(name.getText().equals("Nguyễn Hoài An"), "loads display name");
        check(birth.getText().equals("12/03/2004"), "formats birth date");
        check(gender.getSelectedItem().toString().equals("Nữ"), "loads gender");
        check(!save.isEnabled() && !reset.isEnabled(), "buttons disabled without changes");

        name.setText("   ");
        check(save.isEnabled(), "save enabled after edit");
        save.doClick();
        check(sent.isEmpty(), "blank name rejected");

        name.setText("Nguyễn An");
        birth.setText("31/02/2004");
        save.doClick();
        check(sent.isEmpty(), "impossible date rejected");

        birth.setText(LocalDate.now().plusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        save.doClick();
        check(sent.isEmpty(), "future date rejected");

        birth.setText("01/01/2004");
        avatar.setText("ftp://example.com/a.png");
        save.doClick();
        check(sent.isEmpty(), "non-http avatar rejected");

        avatar.setText("");
        address.setText("");
        gender.setSelectedIndex(3);
        save.doClick();
        check(sent.size() == 1, "valid form submitted");
        ProfileUpdateRequest request = sent.get(0);
        check(request.displayName().equals("Nguyễn An")
                && request.avatarUrl() == null
                && request.address() == null
                && request.dateOfBirth().equals(LocalDate.of(2004, 1, 1))
                && request.gender() == Gender.OTHER, "request matches form");
        check(!save.isEnabled(), "save disabled while saving");

        panel.setSaveFailed("Lỗi thử");
        check(save.isEnabled(), "retry possible after failure");
        panel.setUser(me);
        check(name.getText().equals("Nguyễn An"), "background refresh keeps unsaved edits");

        User updated = store.updateProfile(request);
        panel.setSaveSucceeded(updated);
        check(store.currentUser().displayName().equals("Nguyễn An")
                && store.currentUser().gender() == Gender.OTHER, "store updated");
        check(!save.isEnabled() && name.getText().equals("Nguyễn An"), "saved values become baseline");

        name.setText("Tên tạm");
        reset.doClick();
        check(name.getText().equals("Nguyễn An"), "reset restores saved values");
    }

    private static <T extends Component> List<T> find(Container root, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) result.add(type.cast(child));
            if (child instanceof Container container) result.addAll(find(container, type));
        }
        return result;
    }

    private static JButton button(Container root, String text) {
        return find(root, JButton.class).stream().filter(b -> b.getText().equals(text)).findFirst().orElseThrow();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
