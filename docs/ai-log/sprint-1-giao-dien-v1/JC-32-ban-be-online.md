# JC-32 – Màn hình bạn bè đang online

Ngày 18/11/2026 · Module `user-client` · Java Swing

## Yêu cầu

Hiển thị những bạn bè đang online. Ticket Jira chưa có mô tả chi tiết. `common` có `PresenceStatus` và `PresenceChangedEvent` (`USER_PRESENCE_CHANGED_EVENT`) nhưng chưa có DTO trả về danh sách online ban đầu, nên màn hình nhận tập ID đang online từ tầng ứng dụng và cập nhật theo từng sự kiện.

## AI đã thực hiện

- Thêm `OnlineFriendsPanel`: lọc bạn bè trong `FriendListResponse` theo trạng thái online, sắp xếp theo tên, có chấm xanh trên avatar, username và thời gian đã online ("Vừa online", "Online 5 phút"...).
- Hiển thị số người online trên tổng số bạn bè, trạng thái rỗng khi chưa có bạn bè hoặc không ai online.
- Nút **Nhắn tin** hoặc nhấp đúp dùng chung callback mở hội thoại với màn hình bạn bè (JC-30).
- Gắn nút **Đang online** vào điều hướng `MainFrame`; thêm `setOnlineUserIds(...)` và `applyPresence(PresenceChangedEvent)`.
- Chế độ `--demo`: Minh Anh online sẵn, Gia Huy đổi online/offline mỗi 10 giây bằng `javax.swing.Timer` để thấy cập nhật trực tiếp.
- Cập nhật README.

## Kiểm tra

Biên dịch `common` và `user-client` bằng `javac -encoding UTF-8` trên JDK 21; chạy `UserClientApp --demo` 14 giây không có exception. Máy hiện không có lệnh `mvn`.

## Còn thiếu

Server cần gửi danh sách bạn bè đang online lúc đăng nhập (chưa có DTO trong `common`).
