package com.javachat.user.ui.groups;

import com.javachat.shared.dto.GroupDtos.GroupCreateRequest;
import com.javachat.shared.model.User;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Form for JC-34. Submits data through a callback and performs no network work. */
public final class GroupCreatePanel extends JPanel {
    private static final Color INK = new Color(30, 48, 72);
    private static final Color MUTED = new Color(113, 132, 157);
    private static final Color BLUE = new Color(45, 112, 220);
    private static final Color BORDER = new Color(220, 231, 243);
    private final JTextField name = field();
    private final JTextArea description = new JTextArea(3, 20);
    private final JPanel memberList = new JPanel();
    private final JLabel validation = new JLabel(" ");
    private final List<MemberChoice> choices = new ArrayList<>();
    private Consumer<GroupCreateRequest> createListener = ignored -> { };

    public GroupCreatePanel() {
        super(new BorderLayout());
        setBackground(new Color(238, 245, 252));
        setBorder(BorderFactory.createEmptyBorder(34, 50, 34, 50));
        JPanel card = new JPanel(new BorderLayout(0, 20));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(28, 34, 28, 34)));
        card.add(header("Tạo nhóm chat", "Đặt tên nhóm và chọn ít nhất một thành viên."), BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        addLabel(form, "Tên nhóm *");
        name.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        form.add(name);
        form.add(Box.createVerticalStrut(16));
        addLabel(form, "Mô tả");
        description.setLineWrap(true); description.setWrapStyleWord(true);
        description.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        description.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JScrollPane descriptionScroll = new JScrollPane(description);
        descriptionScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        descriptionScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        form.add(descriptionScroll);
        form.add(Box.createVerticalStrut(20));
        addLabel(form, "Chọn thành viên");
        memberList.setLayout(new BoxLayout(memberList, BoxLayout.Y_AXIS));
        memberList.setBackground(Color.WHITE);
        JScrollPane members = new JScrollPane(memberList);
        members.setBorder(BorderFactory.createLineBorder(BORDER));
        members.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        members.setPreferredSize(new Dimension(500, 190));
        form.add(members);
        card.add(form, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        validation.setForeground(new Color(190, 55, 55));
        JButton create = primary("Tạo nhóm");
        create.addActionListener(event -> submit());
        footer.add(validation, BorderLayout.CENTER);
        footer.add(create, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    public void setAvailableMembers(List<User> users) {
        choices.clear(); memberList.removeAll();
        if (users != null) for (User user : users) {
            MemberChoice choice = new MemberChoice(user);
            choices.add(choice); memberList.add(choice);
        }
        memberList.revalidate(); memberList.repaint();
    }

    public void setCreateListener(Consumer<GroupCreateRequest> listener) {
        createListener = listener == null ? ignored -> { } : listener;
    }

    public void clearForm() {
        name.setText(""); description.setText(""); validation.setText(" ");
        choices.forEach(choice -> choice.selected.setSelected(false));
    }

    private void submit() {
        String groupName = name.getText().strip();
        List<Long> ids = choices.stream().filter(c -> c.selected.isSelected()).map(c -> c.user.id()).toList();
        if (groupName.isEmpty()) { validation.setText("Vui lòng nhập tên nhóm."); name.requestFocusInWindow(); return; }
        if (ids.isEmpty()) { validation.setText("Vui lòng chọn ít nhất một thành viên."); return; }
        validation.setText(" ");
        createListener.accept(new GroupCreateRequest(groupName, description.getText().strip(), null, ids));
    }

    private static JPanel header(String title, String subtitle) {
        JPanel panel = new JPanel(); panel.setOpaque(false); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel(title); heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26)); heading.setForeground(INK);
        JLabel hint = new JLabel(subtitle); hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14)); hint.setForeground(MUTED);
        panel.add(heading); panel.add(Box.createVerticalStrut(7)); panel.add(hint); return panel;
    }
    private static void addLabel(JPanel panel, String text) {
        JLabel label = new JLabel(text); label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13)); label.setForeground(INK);
        label.setAlignmentX(LEFT_ALIGNMENT); panel.add(label); panel.add(Box.createVerticalStrut(7));
    }
    private static JTextField field() {
        JTextField field = new JTextField(); field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(9, 12, 9, 12))); return field;
    }
    private static JButton primary(String text) {
        JButton button = new JButton(text); button.setForeground(Color.WHITE); button.setBackground(BLUE);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13)); button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); return button;
    }
    private static final class MemberChoice extends JPanel {
        final User user; final JCheckBox selected = new JCheckBox();
        MemberChoice(User user) {
            super(new BorderLayout(12, 0)); this.user = user; setBackground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            String display = user.displayName() == null || user.displayName().isBlank() ? user.username() : user.displayName();
            JLabel text = new JLabel("<html><b>" + display + "</b><br><font color='#71849D'>@" + user.username() + "</font></html>");
            add(selected, BorderLayout.WEST); add(text, BorderLayout.CENTER);
        }
    }
}
