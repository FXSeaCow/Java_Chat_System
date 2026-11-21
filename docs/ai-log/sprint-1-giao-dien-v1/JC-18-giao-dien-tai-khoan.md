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
