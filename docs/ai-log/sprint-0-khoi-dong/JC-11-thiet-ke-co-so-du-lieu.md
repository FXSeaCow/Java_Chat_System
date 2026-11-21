# JC-11 – Thiết kế cơ sở dữ liệu

## JC-13 – Viết script tạo bảng (sửa lỗi: thiếu thông tin người dùng)

Ngày 18/11/2026

### Yêu cầu

Đề đồ án (trang 2, phân hệ quản trị, mục 1) yêu cầu thông tin người dùng gồm:
tên đăng nhập, họ tên, địa chỉ, ngày sinh, giới tính, email. Bảng `users` ban đầu
chỉ có `username`, `display_name` và `email`, thiếu địa chỉ, ngày sinh và giới tính.

### Kế hoạch của AI

- Giữ `display_name` làm họ tên, không đổi tên cột để tránh ảnh hưởng code hiện có.
- Thêm ba cột cho phép `NULL`, để tài khoản cũ và tài khoản admin không bắt buộc
  có dữ liệu; việc bắt buộc nhập do màn hình đăng ký quyết định.
- Cập nhật đồng bộ script, dữ liệu mẫu, ERD, model và DTO trong `common`.

### AI đã làm gì

- `database/schema.sql`: thêm `address VARCHAR(255)`, `date_of_birth DATE`,
  `gender VARCHAR(10)` với ràng buộc `ck_users_gender` (`MALE`, `FEMALE`, `OTHER`);
  ghi chú `display_name` là họ tên.
- `database/seed.sql`: thêm địa chỉ, ngày sinh, giới tính cho 8 người dùng mẫu.
- `docs/database/erd.md`: thêm ba trường vào `USERS` và quy tắc ánh xạ thông tin
  người dùng theo đề. `erd.svg` giữ nguyên bản cũ (xuất từ Mermaid Chart, giao diện
  "neo"), chỉ chèn ba dòng mới vào bảng `USERS` bằng script sửa SVG và kéo dài khung
  bảng; các bảng và đường nối khác không đổi.
- `common`: thêm enum `Gender`; thêm `address`, `dateOfBirth` (`LocalDate`),
  `gender` vào `User`, `RegisterRequest` và `ProfileUpdateRequest`.
- `docs/protocol/json-message-types.md`: ghi định dạng `yyyy-MM-dd` cho ngày sinh
  và giá trị hợp lệ của `gender`.
- `user-client`: sửa chỗ tạo `User` trong chế độ demo cho khớp record mới.

### Kiểm tra

Biên dịch `common`, `server`, `user-client`, `admin-client` bằng `javac -encoding UTF-8`
trên JDK 21. Chưa chạy được `schema.sql`/`seed.sql` vì máy không có MySQL.

### Việc còn lại cho các task khác

- JC-20 (màn hình đăng ký, đã xong): cần thêm ô nhập địa chỉ, ngày sinh, giới tính.
- JC-22, JC-37, JC-38: hiển thị và cho sửa ba trường mới.
- JC-73: admin chưa có DTO thêm/sửa người dùng.
