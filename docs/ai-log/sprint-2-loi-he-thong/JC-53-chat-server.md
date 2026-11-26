# JC-53 – Chat server

Sprint 2 – Lõi hệ thống · Module `server`, `common`, `user-client`

> Phạm vi ghi nhận: JC-54 đến JC-57. Các thay đổi trong repo chưa được kiểm thử
> đầu cuối với PostgreSQL thật; không suy ra trạng thái Done trên Jira từ log này.

## JC-54 – ServerSocket nhận kết nối client

### Yêu cầu

Server nhận nhiều kết nối TCP từ client bằng `ServerSocket`, không dùng thư viện
socket bên thứ ba.

### Kế hoạch của AI

Giữ HTTP API hiện tại để ứng dụng desktop vẫn hoạt động; mở cổng TCP riêng trên
loopback, tự đọc và ghi khung tin JSON, xử lý từng client độc lập.

### AI đã làm gì

- Thêm `TcpServer` dùng `java.net.ServerSocket` của JDK, mỗi kết nối chạy trên
  virtual thread; khung tin gồm độ dài 4 byte big-endian và JSON UTF-8, tối đa
  1 MB.
- Thêm cổng `server.tcp.port` (mặc định 9090), tích hợp khởi động và đóng TCP
  listener trong `ServerApp`.
- Trả `SYSTEM_HELLO_RESPONSE` và `SYSTEM_PONG`; request không được hỗ trợ trả
  `ERROR_RESPONSE`. Cấu trúc listener cũng chứa các bước xác thực và đẩy event
  của JC-55 đến JC-57 vì các phần này dùng chung một kết nối.

### Kiểm tra và giới hạn

- Biên dịch Java 21 bằng `javac`; smoke test TCP mở hai client trên loopback.
- Chưa kiểm thử với PostgreSQL thật. Client desktop vẫn dùng HTTP cho nghiệp vụ
  chat, bạn bè và nhóm.

## JC-55 – Xác thực client khi kết nối

### Yêu cầu

Client phải đăng nhập hoặc dùng lại token phiên hợp lệ trước khi gửi request
cần xác thực qua TCP.

### Kế hoạch của AI

Dùng chung `AccountRepository` với HTTP API, lưu token cho từng kết nối và kiểm
tra lại token trong cơ sở dữ liệu trước mỗi request được bảo vệ.

### AI đã làm gì

- `AUTH_LOGIN_REQUEST` nhận `payload.login` và `payload.password`, trả token và
  `userId` trong `AUTH_LOGIN_RESPONSE`.
- `SYSTEM_HELLO_REQUEST` nhận `authToken` để tiếp tục phiên trên kết nối mới;
  `AUTH_LOGOUT_REQUEST` thu hồi phiên. Request thiếu phiên hoặc token hết hạn
  nhận `ERROR_RESPONSE` cùng `requestId` khi request hợp lệ.
- Thêm `TcpServerSmokeTest` dùng bộ xác thực giả để kiểm tra mật khẩu sai,
  đăng nhập, tiếp tục phiên trên kết nối thứ hai và token bị thu hồi.

### Kiểm tra và giới hạn

- Biên dịch và chạy smoke test bằng JDK 21; chưa thử đăng nhập TCP với
  PostgreSQL thật.
- Cổng TCP chỉ lắng nghe loopback theo cấu hình `ServerApp` hiện tại.

## JC-56 – Quản lý danh sách người dùng online

### Yêu cầu

Khi đăng nhập, client nhận toàn bộ ID bạn bè đang online để hiển thị ngay,
không cần đợi event thay đổi trạng thái.

### Kế hoạch của AI

Theo dõi hoạt động của từng phiên trên server, lấy giao của tập người dùng
online với quan hệ bạn bè đã chấp nhận, rồi đưa danh sách ID vào DTO phía client.

### AI đã làm gì

- `PresenceTracker` lưu lần hoạt động cuối theo token. Một phiên hết hạn hoạt
  động sau 45 giây; nhiều phiên của cùng người dùng được gộp thành một trạng
  thái online.
- `ChatRepository.onlineFriendIds(...)` chỉ trả ID của bạn bè có quan hệ
  `ACCEPTED` và đang online. Login HTTP trả `onlineFriendIds`; snapshot và
  `/api/presence` cung cấp danh sách mới nhất.
- Thêm `onlineFriendIds` vào `FriendListResponse`, đọc dữ liệu trong
  `MockChatStore.fromSnapshot(...)`, rồi gọi `MainFrame.setOnlineUserIds(...)`
  khi danh sách bạn bè được cập nhật.

### Kiểm tra và giới hạn

- `MockChatStoreSmokeTest` kiểm tra danh sách ID được chuyển từ snapshot vào
  `FriendListResponse`; biên dịch các module bằng `javac` 21.
- Chưa kiểm thử truy vấn bạn bè online với PostgreSQL thật.
