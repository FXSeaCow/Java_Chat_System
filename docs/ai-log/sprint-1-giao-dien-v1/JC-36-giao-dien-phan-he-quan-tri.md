# JC-36 – Giao diện phân hệ quản trị

Sprint 1 – Giao diện (V1) · Module `admin-client` · Java Swing

## Tích hợp giao diện quản trị vào nhánh JC36

### Yêu cầu

Đưa các màn hình đã có trên `origin/JC36_Giao-diện-phân-hệ-quản-trị` vào source
đang phát triển cùng phần đăng ký, đăng nhập và tài khoản của `JC-18`. Thực hiện
trực tiếp trên nhánh JC36, không tạo nhánh tích hợp khác.

### Kế hoạch của AI

1. Kiểm tra lịch sử hai nhánh và mô phỏng merge để xác định conflict.
2. Giữ các commit đang có trên JC36 local, merge commit giao diện quản trị từ
   remote, rồi merge `JC-18` vào JC36.
3. Biên dịch ba ứng dụng, bổ sung script chạy admin và hướng dẫn phân biệt giao
   diện mẫu với tính năng đã kết nối database.

### AI đã làm gì

- Cập nhật thông tin nhánh remote và xác nhận hai phép mô phỏng merge không có
  conflict. Nhánh JC36 local đã phân kỳ, nên không dùng fast-forward hay reset.
- Merge `origin/JC36_Giao-diện-phân-hệ-quản-trị` vào JC36 local, giữ 9 file Java
  của admin dashboard. Sau đó merge `JC-18` để giữ luồng tài khoản và Supabase
  của user client trên cùng nhánh.
- Admin dashboard hiện có 6 trang: lịch sử đăng nhập, nhóm chat, báo cáo spam,
  người dùng đăng ký mới, người dùng hoạt động và biểu đồ đăng ký.
- Thêm `run-admin.ps1` để biên dịch `common` và `admin-client` bằng Java 21 và mở
  `AdminClientApp`; cập nhật README với lệnh chạy.

### Kiểm tra và giới hạn

- Biên dịch `common` + `admin-client`, và `common` + `server` + `user-client`
  bằng `javac` 21 sau khi merge.
- Biên dịch đồng thời toàn bộ mã Java và chạy `MockChatStoreSmokeTest`,
  `ProfilePanelSmokeTest`, `ProtocolMessageSmokeTest` ở chế độ headless; tất cả
  kết thúc thành công. `git diff --check` không báo lỗi khoảng trắng.
- Các màn hình admin dùng dữ liệu mẫu trong mã nguồn. Chưa có đăng nhập admin,
  truy vấn Supabase hoặc thao tác ghi database từ admin client.
