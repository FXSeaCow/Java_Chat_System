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

## Đồng bộ giao diện admin với giao diện người dùng

### Yêu cầu

Đổi template phân hệ quản trị theo giao diện desktop của người dùng, nhất là
màu sắc, thanh điều hướng và các nút bấm.

### AI đã làm gì

- Dùng lại bảng màu kem, teal và xanh đậm của user client trong `AdminTheme`.
- Tạo header ứng dụng với nút thu nhỏ, phóng to/khôi phục, đóng và kéo cửa sổ;
  đổi sidebar thành thanh điều hướng teal có trạng thái chọn và hover.
- Đồng bộ nền trang, thẻ nội dung, bảng, ô tìm kiếm, bộ lọc, badge và nút hành
  động trên cả 6 màn hình admin. Nút hành động có trạng thái hover, nhấn và focus.

### Kiểm tra

- Biên dịch `common` và `admin-client` bằng `javac -encoding UTF-8` thành công.
- Render headless và xem lại các trang lịch sử đăng nhập, báo cáo spam và biểu đồ;
  bảng, bộ lọc, nút và biểu đồ hiển thị đúng bảng màu mới.
- `git diff --check` không phát hiện lỗi khoảng trắng.

## Tinh chỉnh thanh điều hướng và bảng quản trị

### Yêu cầu

Thanh điều hướng admin thu gọn mặc định, mở khi rê chuột như phía người dùng;
các bảng căn cột theo nội dung và không cắt dữ liệu khi đổi kích thước.

### AI đã làm gì

- Thêm hiệu ứng mở/thu thanh điều hướng theo chuột. Khi thu gọn chỉ hiện biểu
  tượng; tooltip vẫn cho biết tên đầy đủ của từng trang.
- Đặt độ rộng tối thiểu phù hợp cho cột của cả 6 bảng. Bảng tự phân phối không
  gian dư; nếu cửa sổ quá hẹp, thanh cuộn ngang giữ nội dung đọc được.
- Căn số sang phải, tăng khoảng đệm ô và thêm gợi ý cho ô tìm kiếm.
- Đồng bộ chiều rộng bảng dữ liệu với tiêu đề cột khi viewport thay đổi, tránh
  cột cuối có tiêu đề nhưng dữ liệu bị khuất.

### Kiểm tra

- Biên dịch `common` và `admin-client` bằng Java 21 thành công.
- Render headless các trang; kiểm tra lịch sử đăng nhập, nhóm, báo cáo spam và
  người dùng hoạt động. Cột cuối hiển thị đầy đủ sau khi giãn bảng.
- `git diff --check` không phát hiện lỗi khoảng trắng.

## Kết nối phân hệ quản trị với Supabase

### Yêu cầu

Thay dữ liệu mẫu của giao diện quản trị bằng dữ liệu thật từ Supabase giống
luồng người dùng.

### AI đã làm gì

- Thêm đăng nhập riêng cho tài khoản có `role = 'ADMIN'` và kiểm tra vai trò ở
  mọi API admin. Client chỉ giữ token phiên; thông tin JDBC ở server.
- Thêm API snapshot quản trị cho lịch sử đăng nhập, nhóm, báo cáo spam, tài
  khoản mới, người dùng có phiên còn hiệu lực và thống kê theo ngày.
- Thay dữ liệu mẫu ở cả 6 trang bằng kết quả API. Thêm nút làm mới, đăng xuất,
  lọc thời gian và ghi trạng thái xử lý báo cáo vào `public.reports`.
- Cập nhật README cách tạo tài khoản admin qua đăng ký live và đổi `role` trong
  Supabase SQL Editor. Không tạo tài khoản hoặc thay đổi dữ liệu thật khi kiểm tra.

### Kiểm tra và giới hạn

- Biên dịch `common` + `server` và `common` + `admin-client` bằng Java 21.
- Chạy truy vấn snapshot chỉ đọc trên Supabase cấu hình sẵn: trả về 9 phiên,
  2 nhóm, 3 báo cáo, 7 người dùng, 1 người có phiên hiệu lực và 90 ngày thống kê.
- Render headless 6 trang với dữ liệu rỗng và dữ liệu biểu đồ mẫu.
- API `/api/admin/snapshot` từ chối yêu cầu thiếu token với HTTP 401; đăng nhập
  admin sai thông tin trả HTTP 401.
- Chưa kiểm thử luồng đăng nhập admin bằng tài khoản thật vì không có thông tin
  đăng nhập admin hợp lệ trong phiên làm việc; các tài khoản seed dùng hash cũ.
