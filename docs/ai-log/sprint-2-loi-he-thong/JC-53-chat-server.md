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
