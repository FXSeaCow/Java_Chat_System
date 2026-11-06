# JSON Protocol V1 - Message Types

Tài liệu này là nguồn chuẩn cho tên message trao đổi giữa Java client và server.
Code phía client và server phải dùng `MessageType` trong module `shared`, không tự
khai báo chuỗi message riêng.

## 1. Envelope chung

Mọi message sử dụng cùng một cấu trúc ngoài:

```json
{
  "version": "1.0",
  "type": "AUTH_LOGIN_REQUEST",
  "requestId": "a2a89138-c75f-46d8-9305-312c1ad61541",
  "timestamp": "2026-11-06T08:30:00Z",
  "authToken": null,
  "payload": {
    "username": "an.nguyen",
    "password": "password"
  }
}
```

| Field | Kiểu | Bắt buộc | Quy tắc |
|---|---|---:|---|
| `version` | string | Có | Phiên bản hiện tại là `1.0`. |
| `type` | string | Có | Một giá trị trong `MessageType`. |
| `requestId` | UUID string | Request/response | Response và error giữ nguyên ID của request; event có thể để `null`. |
| `timestamp` | ISO-8601 string | Có | Thời gian UTC, ví dụ `2026-11-06T08:30:00Z`. |
| `authToken` | string/null | Tùy loại | Không gửi trong login/register; bắt buộc với request cần đăng nhập. |
| `payload` | object/null | Có | Dữ liệu riêng của message; dùng `{}` nếu không có dữ liệu. |

## 2. Nhóm message

- `REQUEST`: client yêu cầu server thực hiện một thao tác.
- `RESPONSE`: server trả kết quả cho đúng `requestId`.
- `EVENT`: server chủ động đẩy thay đổi đến các client liên quan.
- `ERROR`: request thất bại ở tầng protocol hoặc nghiệp vụ.

Quy ước đặt tên:

```text
<DOMAIN>_<ACTION>_<REQUEST|RESPONSE|EVENT>
```

Các event gõ phím (`TYPING_*`) chỉ được client gửi lên server và server chuyển tiếp;
chúng không được lưu vào cơ sở dữ liệu.

## 3. Catalog message

### 3.1 System

| Type | Kind | Mục đích |
|---|---|---|
| `SYSTEM_HELLO_REQUEST` | Request | Client khai báo protocol version và thông tin ứng dụng. |
| `SYSTEM_HELLO_RESPONSE` | Response | Server xác nhận version và khả năng được hỗ trợ. |
| `SYSTEM_PING` | Request | Kiểm tra kết nối còn hoạt động. |
| `SYSTEM_PONG` | Response | Phản hồi heartbeat. |

### 3.2 Authentication và profile

| Type | Kind | Mục đích |
|---|---|---|
| `AUTH_REGISTER_REQUEST` / `AUTH_REGISTER_RESPONSE` | Request/Response | Đăng ký tài khoản. |
| `AUTH_LOGIN_REQUEST` / `AUTH_LOGIN_RESPONSE` | Request/Response | Đăng nhập và nhận token phiên. |
| `AUTH_LOGOUT_REQUEST` / `AUTH_LOGOUT_RESPONSE` | Request/Response | Đăng xuất và thu hồi token. |
| `AUTH_SESSION_EXPIRED_EVENT` | Event | Báo client phiên đã hết hạn hoặc bị thu hồi. |
| `PROFILE_GET_REQUEST` / `PROFILE_GET_RESPONSE` | Request/Response | Lấy hồ sơ người dùng. |
| `PROFILE_UPDATE_REQUEST` / `PROFILE_UPDATE_RESPONSE` | Request/Response | Cập nhật tên hiển thị và avatar. |
| `USER_PRESENCE_CHANGED_EVENT` | Event | Trạng thái online/offline của người dùng thay đổi. |

### 3.3 Friends

| Type | Kind | Mục đích |
|---|---|---|
| `FRIEND_LIST_REQUEST` / `FRIEND_LIST_RESPONSE` | Request/Response | Lấy danh sách bạn bè và lời mời. |
| `FRIEND_REQUEST_SEND_REQUEST` / `FRIEND_REQUEST_SEND_RESPONSE` | Request/Response | Gửi lời mời kết bạn. |
| `FRIEND_REQUEST_RECEIVED_EVENT` | Event | Người nhận nhận lời mời mới. |
| `FRIEND_REQUEST_RESPOND_REQUEST` / `FRIEND_REQUEST_RESPOND_RESPONSE` | Request/Response | Chấp nhận hoặc từ chối lời mời. |
| `FRIEND_REQUEST_UPDATED_EVENT` | Event | Báo bên còn lại trạng thái lời mời thay đổi. |
| `FRIEND_REMOVE_REQUEST` / `FRIEND_REMOVE_RESPONSE` | Request/Response | Hủy kết bạn. |
| `FRIEND_BLOCK_REQUEST` / `FRIEND_BLOCK_RESPONSE` | Request/Response | Chặn người dùng. |

### 3.4 Conversations và messages

| Type | Kind | Mục đích |
|---|---|---|
| `CONVERSATION_LIST_REQUEST` / `CONVERSATION_LIST_RESPONSE` | Request/Response | Danh sách hội thoại của user. |
| `CONVERSATION_GET_REQUEST` / `CONVERSATION_GET_RESPONSE` | Request/Response | Chi tiết một hội thoại. |
| `DIRECT_CONVERSATION_CREATE_REQUEST` / `DIRECT_CONVERSATION_CREATE_RESPONSE` | Request/Response | Mở/tìm hội thoại trực tiếp giữa hai user. |
| `MESSAGE_HISTORY_REQUEST` / `MESSAGE_HISTORY_RESPONSE` | Request/Response | Lấy lịch sử tin nhắn theo cursor. |
| `MESSAGE_SEND_REQUEST` / `MESSAGE_SEND_RESPONSE` | Request/Response | Gửi tin nhắn và xác nhận bản ghi đã tạo. |
| `MESSAGE_CREATED_EVENT` | Event | Đẩy tin nhắn mới đến thành viên khác. |
| `MESSAGE_EDIT_REQUEST` / `MESSAGE_EDIT_RESPONSE` | Request/Response | Sửa nội dung tin nhắn. |
| `MESSAGE_UPDATED_EVENT` | Event | Đẩy nội dung đã sửa. |
| `MESSAGE_DELETE_REQUEST` / `MESSAGE_DELETE_RESPONSE` | Request/Response | Xóa mềm tin nhắn. |
| `MESSAGE_DELETED_EVENT` | Event | Báo tin nhắn đã bị xóa. |
| `MESSAGE_READ_REQUEST` / `MESSAGE_READ_RESPONSE` | Request/Response | Ghi nhận user đã đọc tin nhắn. |
| `MESSAGE_READ_EVENT` | Event | Đẩy read receipt đến thành viên liên quan. |
| `TYPING_STARTED_EVENT` / `TYPING_STOPPED_EVENT` | Event | Trạng thái đang gõ, không lưu DB. |

### 3.5 Groups

| Type | Kind | Mục đích |
|---|---|---|
| `GROUP_CREATE_REQUEST` / `GROUP_CREATE_RESPONSE` | Request/Response | Tạo nhóm và danh sách thành viên đầu tiên. |
| `GROUP_UPDATE_REQUEST` / `GROUP_UPDATE_RESPONSE` | Request/Response | Sửa tên, avatar hoặc mô tả nhóm. |
| `GROUP_UPDATED_EVENT` | Event | Đẩy thông tin nhóm mới. |
| `GROUP_DELETE_REQUEST` / `GROUP_DELETE_RESPONSE` | Request/Response | Xóa nhóm. |
| `GROUP_DELETED_EVENT` | Event | Báo nhóm đã bị xóa. |
| `GROUP_MEMBER_LIST_REQUEST` / `GROUP_MEMBER_LIST_RESPONSE` | Request/Response | Lấy danh sách thành viên. |
| `GROUP_MEMBER_ADD_REQUEST` / `GROUP_MEMBER_ADD_RESPONSE` | Request/Response | Thêm thành viên. |
| `GROUP_MEMBER_ADDED_EVENT` | Event | Báo có thành viên mới. |
| `GROUP_MEMBER_REMOVE_REQUEST` / `GROUP_MEMBER_REMOVE_RESPONSE` | Request/Response | Rời nhóm hoặc xóa thành viên. |
| `GROUP_MEMBER_REMOVED_EVENT` | Event | Báo thành viên đã rời/bị xóa. |
| `GROUP_MEMBER_ROLE_UPDATE_REQUEST` / `GROUP_MEMBER_ROLE_UPDATE_RESPONSE` | Request/Response | Đổi vai trò member/admin. |
| `GROUP_MEMBER_ROLE_UPDATED_EVENT` | Event | Đẩy thay đổi vai trò. |

### 3.6 AI assistant

| Type | Kind | Mục đích |
|---|---|---|
| `AI_CHAT_REQUEST` | Request | Gửi prompt và conversation context đến trợ lý AI. |
| `AI_CHAT_RESPONSE` | Response | Trả nội dung AI và token usage. |

### 3.7 Report và admin

| Type | Kind | Mục đích |
|---|---|---|
| `REPORT_CREATE_REQUEST` / `REPORT_CREATE_RESPONSE` | Request/Response | Báo cáo một tin nhắn vi phạm. |
| `ADMIN_REPORT_LIST_REQUEST` / `ADMIN_REPORT_LIST_RESPONSE` | Request/Response | Danh sách báo cáo theo trạng thái. |
| `ADMIN_REPORT_REVIEW_REQUEST` / `ADMIN_REPORT_REVIEW_RESPONSE` | Request/Response | Duyệt hoặc bác báo cáo. |
| `ADMIN_USER_STATUS_UPDATE_REQUEST` / `ADMIN_USER_STATUS_UPDATE_RESPONSE` | Request/Response | Khóa hoặc mở tài khoản. |
| `ADMIN_GROUP_LIST_REQUEST` / `ADMIN_GROUP_LIST_RESPONSE` | Request/Response | Danh sách nhóm cho quản trị. |
| `ADMIN_LOGIN_HISTORY_REQUEST` / `ADMIN_LOGIN_HISTORY_RESPONSE` | Request/Response | Tra cứu lịch sử đăng nhập. |
| `ADMIN_STATS_REQUEST` / `ADMIN_STATS_RESPONSE` | Request/Response | Lấy số liệu tổng quan hệ thống. |

### 3.8 Error

Mọi request thất bại trả `ERROR_RESPONSE` với cùng `requestId`:

```json
{
  "version": "1.0",
  "type": "ERROR_RESPONSE",
  "requestId": "a2a89138-c75f-46d8-9305-312c1ad61541",
  "timestamp": "2026-11-06T08:30:01Z",
  "authToken": null,
  "payload": {
    "code": "AUTH_INVALID_CREDENTIALS",
    "message": "Tên đăng nhập hoặc mật khẩu không đúng.",
    "details": null
  }
}
```

Nhóm mã lỗi tối thiểu:

- `PROTOCOL_UNSUPPORTED_VERSION`, `PROTOCOL_UNKNOWN_MESSAGE_TYPE`,
  `PROTOCOL_INVALID_PAYLOAD`
- `AUTH_REQUIRED`, `AUTH_INVALID_CREDENTIALS`, `AUTH_SESSION_EXPIRED`,
  `AUTH_FORBIDDEN`
- `RESOURCE_NOT_FOUND`, `CONFLICT`, `VALIDATION_ERROR`, `RATE_LIMITED`
- `INTERNAL_ERROR`

## 4. Ví dụ request-response-event

Client gửi tin nhắn:

```json
{
  "version": "1.0",
  "type": "MESSAGE_SEND_REQUEST",
  "requestId": "bb00ab1d-a1e5-4b27-a122-5924a20b49ad",
  "timestamp": "2026-11-06T09:00:00Z",
  "authToken": "session-token",
  "payload": {
    "conversationId": 4,
    "clientMessageId": "00db6d61-7d1e-4c32-9637-4a50e163835d",
    "replyToId": null,
    "messageType": "TEXT",
    "content": "Chào cả nhóm!",
    "attachments": []
  }
}
```

Server xác nhận cho client gửi:

```json
{
  "version": "1.0",
  "type": "MESSAGE_SEND_RESPONSE",
  "requestId": "bb00ab1d-a1e5-4b27-a122-5924a20b49ad",
  "timestamp": "2026-11-06T09:00:00.120Z",
  "authToken": null,
  "payload": {
    "messageId": 101,
    "clientMessageId": "00db6d61-7d1e-4c32-9637-4a50e163835d",
    "createdAt": "2026-11-06T09:00:00.100Z"
  }
}
```

Server đẩy đến các thành viên khác:

```json
{
  "version": "1.0",
  "type": "MESSAGE_CREATED_EVENT",
  "requestId": null,
  "timestamp": "2026-11-06T09:00:00.130Z",
  "authToken": null,
  "payload": {
    "id": 101,
    "conversationId": 4,
    "senderId": 1,
    "replyToId": null,
    "messageType": "TEXT",
    "content": "Chào cả nhóm!",
    "status": "SENT",
    "createdAt": "2026-11-06T09:00:00.100Z",
    "attachments": []
  }
}
```

## 5. Quy tắc tương thích

- Server từ chối major version không hỗ trợ bằng `PROTOCOL_UNSUPPORTED_VERSION`.
- Có thể bổ sung field mới vào `payload`; bên nhận phải bỏ qua field chưa biết.
- Không đổi ý nghĩa hoặc xóa field trong cùng major version.
- Không tái sử dụng tên message đã ngừng hỗ trợ cho mục đích khác.
- Password chỉ xuất hiện trong request đăng nhập/đăng ký và không bao giờ được
  ghi log hoặc gửi ngược trong response.
