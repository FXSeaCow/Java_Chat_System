# JC-24 – Giao diện người dùng – Chat

Sprint 1 – Giao diện (V1) · Module `user-client` · Java Swing

Subtask: JC-25 Màn hình chính và danh sách hội thoại · JC-26 Khung chat ·
JC-27 Màn hình tìm kiếm trong lịch sử chat · JC-28 Panel gợi ý chat bằng LLM

> Code của các subtask được tạo trong các phiên làm việc với AI trước đó. Log này
> được viết ngày 11/11/2026, dựa trên code, khi tách commit theo từng subtask.

## JC-25 – Màn hình chính và danh sách hội thoại

### Yêu cầu

Cửa sổ chính của ứng dụng người dùng: danh sách hội thoại bên trái, vùng nội dung
bên phải. Danh sách tìm kiếm được và lọc được hội thoại chưa đọc.

### Kế hoạch của AI

1. Dùng Java Swing (có sẵn trong JDK, không cần thư viện ngoài).
2. `MainFrame` là khung chính: danh sách ở bên trái, vùng chi tiết ở giữa.
3. `ConversationListPanel` nhận `List<ConversationSummary>` (DTO của
   `CONVERSATION_LIST_RESPONSE`), nên khi nối với server chỉ cần truyền dữ liệu vào.
4. UI không gọi mạng. Mọi hành động đi qua listener (callback) để phần network
   gắn vào sau.
5. Chế độ `--demo` dùng dữ liệu mẫu để xem giao diện khi chưa có server.

### AI đã làm gì

- `ConversationListPanel`
  - Ô "Tìm hội thoại" lọc theo tên hoặc nội dung tin cuối khi đang gõ.
  - Hai nút lọc "Tất cả" và "Chưa đọc".
  - Sắp xếp theo hoạt động gần nhất. Mỗi dòng có avatar, tên, tin cuối, thời gian
    và số tin chưa đọc.
  - Có trạng thái rỗng.
- `MainFrame`: màn hình chào mừng khi chưa chọn hội thoại. API gồm
  `setConversations(...)` và `setOpenConversationListener(...)`.
- `UserClientApp`: hàm `main`. Tham số `--demo` nạp 3 hội thoại mẫu.
- README: hướng dẫn chạy demo.
