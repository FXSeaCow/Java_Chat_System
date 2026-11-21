# ERD - Java Chat System

ERD phiên bản 1 mô tả dữ liệu cho các phân hệ: tài khoản, đăng nhập, bạn bè,
chat cá nhân/nhóm, tin nhắn, trợ lý AI và báo cáo vi phạm.

## Sơ đồ trực quan

![ERD Java Chat System](./erd.svg)

## Mermaid source

```mermaid
erDiagram
    direction LR

    USERS ||--o{ AUTH_SESSIONS : "có"
    USERS ||--o{ FRIENDSHIPS : "gửi yêu cầu"
    USERS ||--o{ FRIENDSHIPS : "nhận yêu cầu"
    USERS ||--o{ CONVERSATIONS : "khởi tạo"
    USERS ||--o{ CONVERSATION_MEMBERS : "tham gia"
    CONVERSATIONS ||--|{ CONVERSATION_MEMBERS : "gồm"
    CONVERSATIONS ||--o| GROUP_DETAILS : "có thông tin nhóm"
    USERS ||--o{ GROUP_DETAILS : "sở hữu"
    CONVERSATIONS ||--o{ MESSAGES : "chứa"
    USERS ||--o{ MESSAGES : "gửi"
    MESSAGES o|--o{ MESSAGES : "được trả lời bởi"
    MESSAGES ||--o{ MESSAGE_ATTACHMENTS : "đính kèm"
    MESSAGES ||--o{ MESSAGE_READS : "có lượt đọc"
    USERS ||--o{ MESSAGE_READS : "đọc"
    USERS ||--o{ REPORTS : "gửi báo cáo"
    USERS ||--o{ REPORTS : "xử lý báo cáo"
    MESSAGES ||--o{ REPORTS : "bị báo cáo"
    USERS ||--o{ AI_REQUESTS : "gửi yêu cầu AI"
    CONVERSATIONS ||--o{ AI_REQUESTS : "cung cấp ngữ cảnh"

    USERS {
        bigint id PK
        varchar username UK
        varchar email UK
        varchar password_hash
        varchar display_name
        varchar avatar_url
        varchar address
        date date_of_birth
        varchar gender "MALE FEMALE OTHER"
        varchar role "USER hoặc ADMIN"
        varchar status "ACTIVE BLOCKED DELETED"
        datetime created_at
        datetime updated_at
    }

    AUTH_SESSIONS {
        bigint id PK
        bigint user_id FK
        varchar token_hash UK
        varchar ip_address
        varchar device_info
        datetime logged_in_at
        datetime expires_at
        datetime logged_out_at
    }

    FRIENDSHIPS {
        bigint id PK
        bigint requester_id FK
        bigint addressee_id FK
        varchar status "PENDING ACCEPTED BLOCKED"
        datetime created_at
        datetime responded_at
    }

    CONVERSATIONS {
        bigint id PK
        varchar type "DIRECT hoặc GROUP"
        bigint created_by FK
        datetime created_at
        datetime updated_at
    }

    CONVERSATION_MEMBERS {
        bigint conversation_id PK, FK
        bigint user_id PK, FK
        varchar member_role "MEMBER ADMIN OWNER"
        datetime joined_at
        datetime left_at
        datetime last_read_at
    }

    GROUP_DETAILS {
        bigint conversation_id PK, FK
        bigint owner_id FK
        varchar name
        varchar avatar_url
        text description
        datetime created_at
    }

    MESSAGES {
        bigint id PK
        bigint conversation_id FK
        bigint sender_id FK
        bigint reply_to_id FK
        text content
        varchar message_type "TEXT FILE IMAGE SYSTEM"
        varchar status "SENT EDITED DELETED"
        datetime created_at
        datetime edited_at
    }

    MESSAGE_ATTACHMENTS {
        bigint id PK
        bigint message_id FK
        varchar file_name
        varchar file_url
        varchar mime_type
        bigint file_size
    }

    MESSAGE_READS {
        bigint message_id PK, FK
        bigint user_id PK, FK
        datetime read_at
    }

    REPORTS {
        bigint id PK
        bigint reporter_id FK
        bigint message_id FK
        varchar reason
        text details
        varchar status "PENDING REVIEWED DISMISSED"
        bigint reviewed_by FK
        datetime created_at
        datetime reviewed_at
    }

    AI_REQUESTS {
        bigint id PK
        bigint user_id FK
        bigint conversation_id FK
        text prompt
        text response
        varchar model
        int input_tokens
        int output_tokens
        varchar status
        datetime created_at
    }
```

## Quy tắc và ràng buộc chính

- `USERS.username` và `USERS.email` là duy nhất.
- Thông tin người dùng theo đề gồm tên đăng nhập (`username`), họ tên
  (`display_name`), địa chỉ (`address`), ngày sinh (`date_of_birth`), giới tính
  (`gender`) và email.
- Một cặp người dùng chỉ có một bản ghi trong `FRIENDSHIPS`; hai người không
  được trùng nhau trong cùng một quan hệ.
- `CONVERSATIONS.type` phân biệt chat cá nhân (`DIRECT`) và chat nhóm (`GROUP`).
- Hội thoại nhóm có đúng một bản ghi `GROUP_DETAILS`; hội thoại cá nhân không có.
- `CONVERSATION_MEMBERS` là bảng nối người dùng với hội thoại. Khóa chính kép
  ngăn một người tham gia cùng một hội thoại nhiều lần.
- `MESSAGE_READS` dùng khóa chính kép để mỗi người chỉ có một trạng thái đọc cho
  mỗi tin nhắn.
- `MESSAGES.reply_to_id` có thể rỗng và tham chiếu lại `MESSAGES.id`.
- `REPORTS.reviewed_by` có thể rỗng cho tới khi quản trị viên xử lý báo cáo.
- `AI_REQUESTS.conversation_id` có thể rỗng nếu yêu cầu AI không thuộc một cuộc
  trò chuyện cụ thể.

## Giả định của phiên bản 1

- Quản trị viên được biểu diễn bằng `USERS.role`, chưa tách thành bảng riêng.
- Thành viên và vai trò trong nhóm được quản lý chung bằng
  `CONVERSATION_MEMBERS.member_role`.
- File đính kèm được lưu ở hệ thống file/object storage; cơ sở dữ liệu chỉ giữ
  metadata và đường dẫn.
- Xóa người dùng và tin nhắn là xóa mềm thông qua trường `status`.

