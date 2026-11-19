# JC-31 – Màn hình yêu cầu kết bạn

Ngày 11/11/2026 · Module `user-client` · Java Swing

## Yêu cầu

Hiển thị lời mời kết bạn đã nhận và đã gửi. Ticket Jira chưa có mô tả chi tiết, nên giao diện dùng dữ liệu và hành động trong `FriendDtos` hiện có.

## AI đã thực hiện

- Thêm `FriendRequestsPanel` với hai danh sách đã nhận và đã gửi, sắp xếp theo ngày mới nhất, trạng thái rỗng và tên người dùng khi có hồ sơ.
- Lời mời đã nhận có nút **Chấp nhận** và **Từ chối**, phát `RespondRequest` qua callback. Khóa thao tác trong lúc chờ cập nhật để tránh gửi lặp; có API mở lại khi phản hồi thất bại.
- Gắn màn hình vào điều hướng `MainFrame` và cập nhật cùng `FriendListResponse` với danh sách bạn bè.
- Chế độ `--demo` có lời mời mẫu; chấp nhận thêm bạn bè và hội thoại mẫu.

## Kiểm tra

Biên dịch `common` và `user-client` bằng `javac -encoding UTF-8` trên JDK 21. Máy hiện không có lệnh `mvn`.
