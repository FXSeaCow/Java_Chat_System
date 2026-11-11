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

## JC-26 – Khung chat

### Yêu cầu

Khi chọn một hội thoại: hiện tiêu đề, danh sách tin nhắn và ô nhập để gửi tin.

### Kế hoạch của AI

1. Tạo `ChatPanel` riêng và gắn vào vùng chi tiết của `MainFrame`.
2. Dùng đúng kiểu dữ liệu của `common`: nhận `ChatMessage`, gửi `MessageSendRequest`.
3. Nút Gửi chỉ gọi listener do bên ngoài cung cấp, UI không tự gửi qua mạng.
4. Không thêm trùng tin nhắn khi server gửi lại cùng một ID.

### AI đã làm gì

- `ChatPanel`
  - Header có avatar, tên và loại hội thoại.
  - Bong bóng tin nhắn: tin của mình bên phải, của người khác bên trái, tin hệ
    thống ở giữa. Có giờ gửi và nhãn phân cách ngày.
  - Tin đã xoá hiện "Tin nhắn đã bị xóa".
  - Ô nhập: Enter để gửi, Shift+Enter để xuống dòng. Nút Gửi bị khoá khi ô trống.
  - Tự cuộn xuống tin mới nhất.
- `MainFrame`: thêm `setCurrentUserId`, `setMessages`, `appendMessage`,
  `setSendMessageListener`.
- `UserClientApp`: demo có lịch sử tin nhắn mẫu, gửi tin thì hiện ngay.

## JC-27 – Màn hình tìm kiếm trong lịch sử chat

### Yêu cầu

Tìm chuỗi trong lịch sử chat của hội thoại đang mở. Bấm vào kết quả thì nhảy tới
tin nhắn đó.

### Kế hoạch của AI

1. Tạo `HistorySearchPanel`, mở bằng nút kính lúp trên header khung chat.
2. Ở bản giao diện, chỉ tìm trong các tin đã tải trên client. Tìm trên server
   (JC-86) và trên mọi hội thoại (JC-87) thuộc Sprint 3.
3. Tìm không phân biệt hoa thường và không phân biệt dấu tiếng Việt.
4. Bấm vào kết quả thì cuộn tới tin nhắn và tô viền tin đó.

### AI đã làm gì

- `HistorySearchPanel`
  - Ô tìm kiếm, dòng phạm vi và số kết quả.
  - Bỏ dấu bằng `java.text.Normalizer` trước khi so khớp.
  - Bỏ qua tin đã xoá và tin hệ thống.
  - Mỗi kết quả có người gửi, đoạn trích và thời gian.
- `ChatPanel`
  - Nút kính lúp bật/tắt màn hình tìm kiếm.
  - `focusMessage(id)` cuộn tới tin nhắn và tô viền vàng.
