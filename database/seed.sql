-- Java Chat System - Sample data
-- Target: MySQL 8.0.16+
-- Run database/schema.sql before this file.
-- Demo password for every account: password

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM ai_requests;
DELETE FROM reports;
DELETE FROM message_reads;
DELETE FROM message_attachments;
DELETE FROM messages;
DELETE FROM group_details;
DELETE FROM conversation_members;
DELETE FROM conversations;
DELETE FROM friendships;
DELETE FROM auth_sessions;
DELETE FROM users;

ALTER TABLE ai_requests AUTO_INCREMENT = 1;
ALTER TABLE reports AUTO_INCREMENT = 1;
ALTER TABLE message_attachments AUTO_INCREMENT = 1;
ALTER TABLE messages AUTO_INCREMENT = 1;
ALTER TABLE conversations AUTO_INCREMENT = 1;
ALTER TABLE friendships AUTO_INCREMENT = 1;
ALTER TABLE auth_sessions AUTO_INCREMENT = 1;
ALTER TABLE users AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

START TRANSACTION;

INSERT INTO users (
    id, username, email, password_hash, display_name, avatar_url,
    role, status, created_at, updated_at
) VALUES
    (1, 'an.nguyen', 'an.nguyen@example.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'Nguyễn Hoài An', 'https://i.pravatar.cc/300?img=1',
     'USER', 'ACTIVE', '2026-10-01 08:00:00.000', '2026-11-05 08:00:00.000'),
    (2, 'bao.tran', 'bao.tran@example.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'Trần Gia Bảo', 'https://i.pravatar.cc/300?img=3',
     'USER', 'ACTIVE', '2026-10-02 09:15:00.000', '2026-11-05 08:05:00.000'),
    (3, 'chi.le', 'chi.le@example.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'Lê Minh Chi', 'https://i.pravatar.cc/300?img=5',
     'USER', 'ACTIVE', '2026-10-03 10:30:00.000', '2026-11-05 08:10:00.000'),
    (4, 'dung.pham', 'dung.pham@example.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'Phạm Anh Dũng', 'https://i.pravatar.cc/300?img=7',
     'USER', 'ACTIVE', '2026-10-04 13:00:00.000', '2026-11-04 21:00:00.000'),
    (5, 'ha.vo', 'ha.vo@example.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'Võ Thanh Hà', 'https://i.pravatar.cc/300?img=9',
     'USER', 'BLOCKED', '2026-10-05 14:20:00.000', '2026-11-04 22:00:00.000'),
    (6, 'khanh.do', 'khanh.do@example.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'Đỗ Minh Khánh', NULL,
     'USER', 'ACTIVE', '2026-10-06 15:45:00.000', '2026-11-05 07:55:00.000'),
    (7, 'admin.linh', 'admin.linh@example.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'Quản trị viên Linh', NULL,
     'ADMIN', 'ACTIVE', '2026-09-01 08:00:00.000', '2026-11-05 07:30:00.000'),
    (8, 'admin.nam', 'admin.nam@example.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'Quản trị viên Nam', NULL,
     'ADMIN', 'ACTIVE', '2026-09-01 08:05:00.000', '2026-11-05 07:35:00.000');

INSERT INTO auth_sessions (
    id, user_id, token_hash, ip_address, device_info,
    logged_in_at, expires_at, logged_out_at
) VALUES
    (1, 1, 'seed-token-hash-user-1', '192.168.1.10', 'Windows 11 - JavaFX',
     '2026-11-05 07:45:00.000', '2026-11-12 07:45:00.000', NULL),
    (2, 2, 'seed-token-hash-user-2', '192.168.1.11', 'Windows 11 - JavaFX',
     '2026-11-05 07:50:00.000', '2026-11-12 07:50:00.000', NULL),
    (3, 3, 'seed-token-hash-user-3', '10.0.0.23', 'Ubuntu 24.04 - JavaFX',
     '2026-11-04 18:00:00.000', '2026-11-11 18:00:00.000',
     '2026-11-04 21:30:00.000'),
    (4, 7, 'seed-token-hash-admin-7', '172.16.0.5', 'Windows 11 - Admin App',
     '2026-11-05 07:30:00.000', '2026-11-05 19:30:00.000', NULL);

INSERT INTO friendships (
    id, requester_id, addressee_id, status, created_at, responded_at
) VALUES
    (1, 1, 2, 'ACCEPTED', '2026-10-10 09:00:00.000', '2026-10-10 09:15:00.000'),
    (2, 1, 3, 'ACCEPTED', '2026-10-11 10:00:00.000', '2026-10-11 10:20:00.000'),
    (3, 2, 4, 'ACCEPTED', '2026-10-12 11:00:00.000', '2026-10-12 11:30:00.000'),
    (4, 3, 4, 'PENDING',  '2026-11-04 19:00:00.000', NULL),
    (5, 1, 5, 'BLOCKED',  '2026-10-15 08:00:00.000', '2026-10-16 08:00:00.000'),
    (6, 2, 6, 'PENDING',  '2026-11-05 07:00:00.000', NULL);

INSERT INTO conversations (id, type, created_by, created_at, updated_at) VALUES
    (1, 'DIRECT', 1, '2026-10-10 09:16:00.000', '2026-11-05 08:12:00.000'),
    (2, 'DIRECT', 1, '2026-10-11 10:21:00.000', '2026-11-04 20:05:00.000'),
    (3, 'DIRECT', 2, '2026-10-12 11:31:00.000', '2026-11-03 18:00:00.000'),
    (4, 'GROUP',  1, '2026-10-20 08:00:00.000', '2026-11-05 08:30:00.000'),
    (5, 'GROUP',  2, '2026-10-22 19:00:00.000', '2026-11-04 22:15:00.000');

INSERT INTO conversation_members (
    conversation_id, user_id, member_role, joined_at, left_at, last_read_at
) VALUES
    (1, 1, 'MEMBER', '2026-10-10 09:16:00.000', NULL, '2026-11-05 08:12:30.000'),
    (1, 2, 'MEMBER', '2026-10-10 09:16:00.000', NULL, '2026-11-05 08:13:00.000'),
    (2, 1, 'MEMBER', '2026-10-11 10:21:00.000', NULL, '2026-11-04 20:06:00.000'),
    (2, 3, 'MEMBER', '2026-10-11 10:21:00.000', NULL, '2026-11-04 20:06:00.000'),
    (3, 2, 'MEMBER', '2026-10-12 11:31:00.000', NULL, '2026-11-03 18:01:00.000'),
    (3, 4, 'MEMBER', '2026-10-12 11:31:00.000', NULL, '2026-11-03 18:01:00.000'),
    (4, 1, 'OWNER',  '2026-10-20 08:00:00.000', NULL, '2026-11-05 08:31:00.000'),
    (4, 2, 'ADMIN',  '2026-10-20 08:05:00.000', NULL, '2026-11-05 08:31:00.000'),
    (4, 3, 'MEMBER', '2026-10-20 08:10:00.000', NULL, '2026-11-05 08:29:00.000'),
    (4, 4, 'MEMBER', '2026-10-21 09:00:00.000', NULL, '2026-11-04 22:00:00.000'),
    (4, 5, 'MEMBER', '2026-10-22 09:00:00.000', '2026-11-04 22:10:00.000',
     '2026-11-04 22:05:00.000'),
    (4, 6, 'MEMBER', '2026-10-23 14:00:00.000', NULL, NULL),
    (5, 2, 'OWNER',  '2026-10-22 19:00:00.000', NULL, '2026-11-04 22:16:00.000'),
    (5, 3, 'ADMIN',  '2026-10-22 19:05:00.000', NULL, '2026-11-04 22:16:00.000'),
    (5, 5, 'MEMBER', '2026-10-22 19:10:00.000', '2026-11-04 22:20:00.000',
     '2026-11-04 22:15:30.000');

INSERT INTO group_details (
    conversation_id, owner_id, name, avatar_url, description, created_at
) VALUES
    (4, 1, 'Java Study Group', 'https://example.com/groups/java-study.png',
     'Cùng học Java, Maven, JDBC và JavaFX.', '2026-10-20 08:00:00.000'),
    (5, 2, 'Chuyện cuối ngày', NULL,
     'Nhóm trò chuyện và chia sẻ sau giờ học.', '2026-10-22 19:00:00.000');

INSERT INTO messages (
    id, conversation_id, sender_id, reply_to_id, content,
    message_type, status, created_at, edited_at
) VALUES
    (1, 1, 1, NULL, 'Chào Bảo, hôm nay bạn học Java đến đâu rồi?',
     'TEXT', 'SENT', '2026-11-05 08:10:00.000', NULL),
    (2, 1, 2, 1, 'Mình đang làm phần JDBC, lát nữa gửi bạn xem nhé.',
     'TEXT', 'SENT', '2026-11-05 08:12:00.000', NULL),
    (3, 2, 3, NULL, 'An ơi, tối nay họp nhóm lúc 20 giờ nhé.',
     'TEXT', 'SENT', '2026-11-04 20:00:00.000', NULL),
    (4, 2, 1, 3, 'Ok Chi, mình sẽ tham gia đúng giờ.',
     'TEXT', 'EDITED', '2026-11-04 20:03:00.000', '2026-11-04 20:05:00.000'),
    (5, 3, 2, NULL, 'Dũng kiểm tra giúp mình phần giao diện đăng nhập nhé.',
     'TEXT', 'SENT', '2026-11-03 17:58:00.000', NULL),
    (6, 4, 1, NULL, 'Mình gửi sơ đồ ERD phiên bản đầu tiên.',
     'TEXT', 'SENT', '2026-11-05 08:20:00.000', NULL),
    (7, 4, 1, 6, NULL,
     'IMAGE', 'SENT', '2026-11-05 08:20:10.000', NULL),
    (8, 4, 2, 6, 'Quan hệ conversation và member nhìn ổn rồi.',
     'TEXT', 'SENT', '2026-11-05 08:24:00.000', NULL),
    (9, 4, 5, NULL, 'Bấm vào link này để nhận quà miễn phí!!!',
     'TEXT', 'SENT', '2026-11-04 21:55:00.000', NULL),
    (10, 4, 7, 9, 'Tin nhắn đã được quản trị viên ghi nhận để kiểm tra.',
     'SYSTEM', 'SENT', '2026-11-04 22:05:00.000', NULL),
    (11, 5, 2, NULL, 'Mọi người hoàn thành task hôm nay chưa?',
     'TEXT', 'SENT', '2026-11-04 22:10:00.000', NULL),
    (12, 5, 3, 11, 'Mình hoàn thành phần giao diện chat rồi.',
     'TEXT', 'SENT', '2026-11-04 22:15:00.000', NULL);

INSERT INTO message_attachments (
    id, message_id, file_name, file_url, mime_type, file_size
) VALUES
    (1, 7, 'java-chat-erd.png',
     'https://example.com/uploads/java-chat-erd.png', 'image/png', 248321),
    (2, 8, 'review-notes.pdf',
     'https://example.com/uploads/review-notes.pdf', 'application/pdf', 512044);

INSERT INTO message_reads (message_id, user_id, read_at) VALUES
    (1, 2, '2026-11-05 08:10:30.000'),
    (2, 1, '2026-11-05 08:12:30.000'),
    (3, 1, '2026-11-04 20:01:00.000'),
    (4, 3, '2026-11-04 20:06:00.000'),
    (6, 2, '2026-11-05 08:21:00.000'),
    (6, 3, '2026-11-05 08:22:00.000'),
    (6, 4, '2026-11-05 08:25:00.000'),
    (7, 2, '2026-11-05 08:21:10.000'),
    (8, 1, '2026-11-05 08:25:00.000'),
    (9, 1, '2026-11-04 21:56:00.000'),
    (9, 2, '2026-11-04 21:57:00.000'),
    (11, 3, '2026-11-04 22:11:00.000'),
    (12, 2, '2026-11-04 22:16:00.000');

INSERT INTO reports (
    id, reporter_id, message_id, reason, details,
    status, reviewed_by, created_at, reviewed_at
) VALUES
    (1, 1, 9, 'SPAM', 'Tin nhắn quảng cáo và có dấu hiệu lừa đảo.',
     'REVIEWED', 7, '2026-11-04 21:58:00.000', '2026-11-04 22:04:00.000'),
    (2, 2, 9, 'SPAM', 'Nội dung không liên quan đến nhóm học tập.',
     'REVIEWED', 7, '2026-11-04 21:59:00.000', '2026-11-04 22:04:30.000'),
    (3, 4, 9, 'OTHER', 'Cần kiểm tra nội dung theo quy định nhóm.',
     'PENDING', NULL, '2026-11-05 07:10:00.000', NULL);

INSERT INTO ai_requests (
    id, user_id, conversation_id, prompt, response, model,
    input_tokens, output_tokens, status, created_at
) VALUES
    (1, 1, 4,
     'Giải thích sự khác nhau giữa khóa chính và khóa ngoại.',
     'Khóa chính định danh duy nhất một bản ghi; khóa ngoại tham chiếu khóa của bảng khác để tạo quan hệ.',
     'demo-chat-model', 18, 34, 'COMPLETED', '2026-11-05 08:00:00.000'),
    (2, 3, 4,
     'Gợi ý các index cần thiết cho bảng messages.',
     'Nên ưu tiên index ghép conversation_id, created_at và index theo sender_id khi cần tra lịch sử gửi.',
     'demo-chat-model', 16, 39, 'COMPLETED', '2026-11-05 08:05:00.000'),
    (3, 2, NULL,
     'Tóm tắt nguyên tắc chuẩn hóa cơ sở dữ liệu.',
     NULL,
     'demo-chat-model', 14, 0, 'PENDING', '2026-11-05 08:28:00.000'),
    (4, 6, 4,
     'Tạo ví dụ truy vấn lấy 20 tin nhắn mới nhất.',
     NULL,
     'demo-chat-model', 17, 0, 'FAILED', '2026-11-05 08:29:00.000');

COMMIT;
