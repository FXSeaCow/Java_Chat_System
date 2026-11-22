# JC-18 – Giao diện người dùng – Tài khoản

Sprint 1 – Giao diện (V1) · Module `user-client` · Java Swing

Subtask: JC-19 Màn hình đăng nhập · JC-20 Màn hình đăng ký · JC-21 Màn hình quên
mật khẩu · JC-22 Màn hình cập nhật thông tin tài khoản

> JC-19 đến JC-21 do thành viên khác làm và đã có trên nhánh `JC-18`. File này
> hiện chỉ ghi phần AI hỗ trợ trong JC-22.

## JC-22 – Màn hình cập nhật thông tin tài khoản

### Yêu cầu

Hiển thị và cho sửa họ tên, avatar, địa chỉ, ngày sinh, giới tính
(`MALE`/`FEMALE`/`OTHER`), khớp với `AuthDtos.ProfileUpdateRequest`.

### Kế hoạch của AI

1. Tạo `ProfilePanel` riêng trong gói `ui/account`. Panel không gọi server: khi
   lưu, nó gửi `ProfileUpdateRequest` qua callback, giống các panel bạn bè và nhóm.
2. Tên đăng nhập và email chỉ hiển thị, không sửa, vì `ProfileUpdateRequest`
   không có hai trường này.
3. Kiểm tra dữ liệu theo giới hạn của bảng `users` trong `database/schema.sql`:
   họ tên bắt buộc và tối đa 100 ký tự, địa chỉ tối đa 255, URL ảnh tối đa 2048
   và phải là `http(s)`, ngày sinh dạng `dd/MM/yyyy`, có thật và trước hôm nay.
   Giới tính có thể để trống, vì cột `gender` cho phép `NULL`.
4. Nút **Lưu** và **Hoàn tác** chỉ bật khi có thay đổi. Khi danh sách được làm
   mới ngầm, panel giữ lại phần người dùng đang sửa dở.
5. Thêm nút **Tài khoản** vào thanh điều hướng của `MainFrame`.
6. Chế độ demo lưu thay đổi vào `MockChatStore`. Server chưa có API cập nhật hồ
   sơ (thuộc JC-68), nên chế độ live báo lỗi rõ ràng thay vì im lặng.

### AI đã làm gì

- `ProfilePanel`: thẻ bên trái có ảnh đại diện tròn (tải ảnh từ URL ở luồng nền,
  timeout 4 giây; nếu không tải được thì hiện chữ cái đầu của họ tên), họ tên,
  tên đăng nhập, email. Form bên phải có báo lỗi dưới từng ô và dòng trạng thái
  khi lưu, lưu thành công hoặc thất bại.
- `MainFrame`: thêm trang `account`, nút **Tài khoản** có icon riêng, các hàm
  `showAccount`, `setCurrentUser`, `setProfileUpdateListener`,
  `setProfileUpdateSucceeded`, `setProfileUpdateFailed`.
- `MockChatStore`: thêm `currentUser()` và `updateProfile(...)`.
- `UserClientApp`: nối listener cập nhật hồ sơ. Demo cập nhật trong bộ nhớ, live
  báo "Server chưa hỗ trợ cập nhật thông tin tài khoản."
- `ProfilePanelSmokeTest`: kiểm tra nạp dữ liệu, từ chối họ tên trống, ngày không
  có thật (31/02), ngày trong tương lai, URL không phải `http(s)`, nội dung
  request gửi đi, giữ phần sửa dở khi làm mới, lưu thành công và hoàn tác.
- Lỗi tìm được khi chạy test: ô ảnh đại diện ban đầu kế thừa `JComponent` nên
  không có `AccessibleContext`, làm app lỗi khi mở. Đã đổi sang `JPanel`.
- Kiểm tra: biên dịch `common` và `user-client` bằng `javac`, chạy
  `ProfilePanelSmokeTest` và `MockChatStoreSmokeTest` ở chế độ headless, render
  màn hình ra ảnh để xem bố cục. Máy không có `mvn` nên chưa chạy `mvn verify`.

## Bổ sung JC-19–JC-22 – Xác thực và bảo mật tài khoản (22/11/2026)

### Yêu cầu

Cho chạy client trên Windows; tài khoản đăng ký phải lưu vào Supabase; đăng nhập
phải dùng tài khoản trong database thay vì user ID cố định; có đăng xuất và đổi
mật khẩu. Cải thiện các nút trên trang Tài khoản và sửa hộp thoại đổi mật khẩu bị
cắt nội dung, có thanh tiêu đề lệch màu.

### Kế hoạch của AI

1. Đưa tài nguyên ảnh vào classpath của script chạy client, giữ chế độ demo độc
   lập với database.
2. Nối form đăng ký/đăng nhập với API local. Lưu hash mật khẩu và phiên đăng nhập
   trong PostgreSQL; các API chat lấy user ID từ token của phiên.
3. Đăng xuất bằng cách hủy phiên trên server và xóa token ở client. Đổi mật khẩu
   sau khi kiểm tra mật khẩu hiện tại; hủy toàn bộ phiên và yêu cầu đăng nhập lại.
4. Thống nhất kiểu nút Swing, sửa bố cục hộp thoại theo kích thước nội dung và
   dùng thanh tiêu đề cùng bảng màu của ứng dụng.

### AI đã làm gì

- Sửa `run-client.ps1` và `run-demo.ps1` để nạp ảnh trong `resources`; lỗi
  `cat-paw-handle.png` khi mở màn hình đăng nhập được xử lý.
- Tạo `AccountRepository` cho đăng ký, đăng nhập, xác thực token, đăng xuất và đổi
  mật khẩu. Tài khoản mới dùng PBKDF2; tài khoản seed dùng bcrypt qua `pgcrypto`
  của Supabase. Token ngẫu nhiên chỉ lưu dạng SHA-256 trong `auth_sessions`.
- Thêm các endpoint `/api/auth/register`, `/api/auth/login`, `/api/auth/logout`
  và `/api/auth/change-password`. Các endpoint chat yêu cầu token và dùng ID của
  tài khoản đã đăng nhập. `MockChatStore` đọc `currentUserId` từ snapshot, không
  cố định user ID 1 trong chế độ live.
- Form đăng ký và đăng nhập gọi API bất đồng bộ, báo lỗi khi server từ chối.
  Đăng xuất đưa người dùng về màn hình đăng nhập; nếu server không phản hồi,
  client vẫn xóa token tại máy và báo rõ phiên trên server chưa được xác nhận hủy.
- Trang Tài khoản có nút Đăng xuất và Đổi mật khẩu. Hộp thoại đổi mật khẩu kiểm
  tra ba trường, cho hiện/ẩn mật khẩu và gửi yêu cầu qua listener. Khi đổi thành
  công, server lưu hash mới, hủy toàn bộ phiên và client yêu cầu đăng nhập lại.
- Các nút thao tác tài khoản được bo góc, có trạng thái hover/nhấn/vô hiệu hóa;
  nút Đăng xuất có màu cảnh báo nhẹ. Hộp thoại đổi mật khẩu bỏ thanh tiêu đề
  Windows màu đỏ, tự tính chiều cao theo nội dung để không cắt ô “Hiện mật khẩu”.
  Nút điều khiển cửa sổ cũng có trạng thái hover.
- Cập nhật `README.md`, `config.properties.example` và chú thích `seed.sql`.
  Hash seed hiện có không khớp mật khẩu `password` ghi trong chú thích cũ.

### Kiểm tra và giới hạn

- Biên dịch `common`, `server` và `user-client` bằng `javac` 21; chạy
  `git diff --check`. Máy chưa có lệnh `mvn` trong PATH.
- Kiểm tra kết nối Supabase: đọc được 8 dòng từ `public.users`; xác nhận có
  `extensions.crypt`. Kiểm tra PBKDF2 với mật khẩu đúng/sai, API từ chối đăng
  nhập sai và yêu cầu không có token bằng HTTP 401.
- Render nhóm nút tài khoản từ mã Swing để kiểm tra màu và khoảng cách. Công cụ
  điều khiển cửa sổ Java bị từ chối quyền truy cập, nên chưa kiểm tra trực tiếp
  toàn bộ hộp thoại sau khi sửa.
- Chưa tạo tài khoản thử hoặc đổi mật khẩu trên database thật để tránh ghi dữ
  liệu thử vào Supabase. Chế độ live chưa hỗ trợ cập nhật hồ sơ và khôi phục mật
  khẩu qua email; các phần này chưa được thay đổi trong đợt này.
