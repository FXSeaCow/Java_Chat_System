# Java Chat System

Ngôn ngữ miền dùng chung cho tài khoản, quan hệ xã hội và trao đổi tin nhắn trong hệ thống.

## Language

**User**:
Một tài khoản có thể đăng nhập, kết bạn và tham gia hội thoại.
_Avoid_: Account, client

**Conversation**:
Không gian chứa thành viên và tin nhắn; có loại trực tiếp hoặc nhóm.
_Avoid_: Chat room, thread

**Direct Conversation**:
Conversation riêng giữa đúng hai user.
_Avoid_: Private chat, inbox

**Group**:
Conversation có thông tin nhóm và nhiều thành viên với vai trò owner, admin hoặc member.
_Avoid_: Channel, room

**Friendship**:
Quan hệ duy nhất giữa hai user, bắt đầu từ lời mời và có trạng thái pending, accepted hoặc blocked.
_Avoid_: Contact, follow

**Message**:
Nội dung được một user gửi vào conversation; có thể trả lời một message khác và có attachment.
_Avoid_: Post, chat

**Read Receipt**:
Bản ghi xác nhận một user đã đọc một message tại một thời điểm.
_Avoid_: Seen flag

**Report**:
Yêu cầu quản trị viên xem xét một message có khả năng vi phạm.
_Avoid_: Complaint, ticket

**AI Request**:
Một lượt user gửi prompt cho trợ lý AI, có thể gắn với context của conversation.
_Avoid_: Bot message, AI chat
