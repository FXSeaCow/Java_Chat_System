package com.javachat.user.ui.groups;

import com.javachat.shared.dto.GroupDtos.GroupUpdateRequest;
import com.javachat.shared.dto.GroupDtos.MemberRemoveRequest;
import com.javachat.shared.dto.GroupDtos.MemberRoleUpdateRequest;
import com.javachat.shared.model.ConversationMember;
import com.javachat.shared.model.GroupDetails;
import com.javachat.shared.model.MemberRole;
import com.javachat.shared.model.User;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Group settings and member administration screen for JC-35. */
public final class GroupManagementPanel extends JPanel {
    private static final Color INK = new Color(30, 48, 72);
    private static final Color MUTED = new Color(113, 132, 157);
    private static final Color BLUE = new Color(45, 112, 220);
    private static final Color BORDER = new Color(220, 231, 243);
    private final JComboBox<GroupDetails> groups = new JComboBox<>();
    private final JTextField name = new JTextField();
    private final JTextArea description = new JTextArea(2, 20);
    private final JPanel memberList = new JPanel();
    private final Map<Long, User> users = new HashMap<>();
    private List<ConversationMember> members = List.of();
    private Consumer<Long> groupSelectedListener = ignored -> { };
    private Consumer<GroupUpdateRequest> updateListener = ignored -> { };
    private Consumer<MemberRemoveRequest> removeListener = ignored -> { };
    private Consumer<MemberRoleUpdateRequest> roleListener = ignored -> { };

    public GroupManagementPanel() {
        super(new BorderLayout());
        setBackground(new Color(238, 245, 252));
        setBorder(BorderFactory.createEmptyBorder(30, 46, 30, 46));
        JPanel card = new JPanel(new BorderLayout(0, 18));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(26, 32, 26, 32)));
        JPanel header = new JPanel(new BorderLayout(16, 0)); header.setOpaque(false);
        JLabel title = new JLabel("Quản lý nhóm chat"); title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25)); title.setForeground(INK);
        groups.setPreferredSize(new Dimension(230, 38));
        groups.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                label.setText(value instanceof GroupDetails details ? details.name() : "Chọn nhóm"); return label;
            }
        });
        groups.addActionListener(event -> selectGroup());
        header.add(title, BorderLayout.WEST); header.add(groups, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(); body.setOpaque(false); body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        label(body, "Tên nhóm"); style(name); name.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); body.add(name);
        body.add(Box.createVerticalStrut(12)); label(body, "Mô tả");
        description.setLineWrap(true); description.setWrapStyleWord(true); description.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        description.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JScrollPane descriptionScroll = new JScrollPane(description); descriptionScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        descriptionScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68)); body.add(descriptionScroll);
        body.add(Box.createVerticalStrut(10));
        JButton save = new JButton("Lưu thay đổi"); save.setForeground(Color.WHITE); save.setBackground(BLUE);
        save.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13)); save.setFocusPainted(false);
        save.setAlignmentX(LEFT_ALIGNMENT); save.addActionListener(event -> save()); body.add(save);
        body.add(Box.createVerticalStrut(20)); label(body, "Thành viên");
        memberList.setLayout(new BoxLayout(memberList, BoxLayout.Y_AXIS)); memberList.setBackground(Color.WHITE);
        JScrollPane memberScroll = new JScrollPane(memberList); memberScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        memberScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260)); memberScroll.setPreferredSize(new Dimension(500, 230));
        body.add(memberScroll); card.add(body, BorderLayout.CENTER); add(card, BorderLayout.CENTER);
    }

    public void setGroups(List<GroupDetails> values) {
        groups.removeAllItems(); if (values != null) values.forEach(groups::addItem);
    }
    public void setUsers(List<User> values) { users.clear(); if (values != null) values.forEach(user -> users.put(user.id(), user)); rebuildMembers(); }
    public void setMembers(List<ConversationMember> values) { members = values == null ? List.of() : List.copyOf(values); rebuildMembers(); }
    public void setGroupSelectedListener(Consumer<Long> listener) { groupSelectedListener = safe(listener); }
    public void setUpdateListener(Consumer<GroupUpdateRequest> listener) { updateListener = safe(listener); }
    public void setRemoveListener(Consumer<MemberRemoveRequest> listener) { removeListener = safe(listener); }
    public void setRoleListener(Consumer<MemberRoleUpdateRequest> listener) { roleListener = safe(listener); }
    private static <T> Consumer<T> safe(Consumer<T> listener) { return listener == null ? ignored -> { } : listener; }

    private void selectGroup() {
        GroupDetails details = (GroupDetails) groups.getSelectedItem();
        if (details == null) { name.setText(""); description.setText(""); return; }
        name.setText(details.name()); description.setText(details.description() == null ? "" : details.description());
        groupSelectedListener.accept(details.conversationId());
    }
    private void save() {
        GroupDetails details = (GroupDetails) groups.getSelectedItem();
        if (details == null || name.getText().isBlank()) return;
        updateListener.accept(new GroupUpdateRequest(details.conversationId(), name.getText().strip(),
                description.getText().strip(), details.avatarUrl()));
    }
    private void rebuildMembers() {
        memberList.removeAll();
        for (ConversationMember member : members) {
            User user = users.get(member.userId());
            String display = user == null ? "Người dùng " + member.userId()
                    : user.displayName() == null || user.displayName().isBlank() ? user.username() : user.displayName();
            JPanel row = new JPanel(new BorderLayout(10, 0)); row.setBackground(Color.WHITE);
            row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                    BorderFactory.createEmptyBorder(9, 12, 9, 12)));
            JLabel person = new JLabel("<html><b>" + display + "</b><br><font color='#71849D'>ID " + member.userId() + "</font></html>");
            row.add(person, BorderLayout.CENTER);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0)); actions.setOpaque(false);
            JComboBox<MemberRole> role = new JComboBox<>(MemberRole.values()); role.setSelectedItem(member.role());
            role.setEnabled(member.role() != MemberRole.OWNER);
            role.addActionListener(event -> {
                MemberRole selected = (MemberRole) role.getSelectedItem();
                if (selected != member.role()) roleListener.accept(new MemberRoleUpdateRequest(member.conversationId(), member.userId(), selected));
            });
            JButton remove = new JButton("Xóa"); remove.setForeground(new Color(190, 55, 55)); remove.setEnabled(member.role() != MemberRole.OWNER);
            remove.addActionListener(event -> removeListener.accept(new MemberRemoveRequest(member.conversationId(), member.userId())));
            actions.add(role); actions.add(remove); row.add(actions, BorderLayout.EAST); memberList.add(row);
        }
        memberList.revalidate(); memberList.repaint();
    }
    private static void label(JPanel panel, String text) {
        JLabel label = new JLabel(text); label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13)); label.setForeground(INK);
        label.setAlignmentX(LEFT_ALIGNMENT); panel.add(label); panel.add(Box.createVerticalStrut(6));
    }
    private static void style(JTextField field) {
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(8, 10, 8, 10)));
    }
}
