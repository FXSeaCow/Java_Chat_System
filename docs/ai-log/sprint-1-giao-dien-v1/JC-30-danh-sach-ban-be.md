# JC-30 – Màn hình danh sách bạn bè

Ngày 11/11/2026 · Module `user-client` · Java Swing

## Yêu cầu

Hiển thị danh sách bạn bè trong giao diện người dùng. Ticket Jira chưa có mô tả hoặc tiêu chí chấp nhận chi tiết; phần này tập trung vào danh sách, còn lời mời kết bạn, bạn bè online và tìm người thuộc JC-31 đến JC-33.

## AI đã thực hiện

- Thêm `FriendListPanel` nhận `FriendListResponse` từ `common`, hiển thị tên và username, sắp xếp theo tên, tìm kiếm không phân biệt hoa thường hoặc dấu tiếng Việt và xử lý trạng thái rỗng.
- Thêm nút **Bạn bè** trong `MainFrame`; **Nhắn tin** hoặc nhấp đúp gọi callback với `User` được chọn để tầng ứng dụng mở hội thoại trực tiếp.
- Thêm hai bạn bè mẫu vào chế độ `--demo`; chọn bạn bè sẽ mở hội thoại mẫu tương ứng.
- Cập nhật README về API tích hợp và cách xem demo.

## Kiểm tra

Biên dịch `common` và `user-client` bằng `javac -encoding UTF-8` trên JDK 21. Máy hiện không có lệnh `mvn`, nên chưa chạy được `mvn verify`.
