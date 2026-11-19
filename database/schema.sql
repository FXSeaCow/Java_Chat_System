-- Java Chat System - Database schema
-- Target: MySQL 8.0.16+
-- Character set utf8mb4 supports Vietnamese text and emoji.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS ai_requests;
DROP TABLE IF EXISTS reports;
DROP TABLE IF EXISTS message_reads;
DROP TABLE IF EXISTS message_attachments;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS group_details;
DROP TABLE IF EXISTS conversation_members;
DROP TABLE IF EXISTS conversations;
DROP TABLE IF EXISTS friendships;
DROP TABLE IF EXISTS auth_sessions;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100) NOT NULL,          -- họ tên
    avatar_url      VARCHAR(2048) NULL,
    address         VARCHAR(255) NULL,
    date_of_birth   DATE NULL,
    gender          VARCHAR(10) NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'USER',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                                  ON UPDATE CURRENT_TIMESTAMP(3),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'DELETED')),
    CONSTRAINT ck_users_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE auth_sessions (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL,
    token_hash      VARCHAR(255) NOT NULL,
    ip_address      VARCHAR(45) NULL,
    device_info     VARCHAR(500) NULL,
    logged_in_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    expires_at      DATETIME(3) NOT NULL,
    logged_out_at   DATETIME(3) NULL,

    CONSTRAINT pk_auth_sessions PRIMARY KEY (id),
    CONSTRAINT uk_auth_sessions_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_auth_sessions_expiry CHECK (expires_at > logged_in_at),
    CONSTRAINT fk_auth_sessions_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE CASCADE,

    INDEX idx_auth_sessions_user_active (user_id, logged_out_at, expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE friendships (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    requester_id    BIGINT UNSIGNED NOT NULL,
    addressee_id    BIGINT UNSIGNED NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    responded_at    DATETIME(3) NULL,
    user_a_id       BIGINT UNSIGNED GENERATED ALWAYS AS
                        (LEAST(requester_id, addressee_id)) STORED,
    user_b_id       BIGINT UNSIGNED GENERATED ALWAYS AS
                        (GREATEST(requester_id, addressee_id)) STORED,

    CONSTRAINT pk_friendships PRIMARY KEY (id),
    CONSTRAINT uk_friendships_pair UNIQUE (user_a_id, user_b_id),
    CONSTRAINT ck_friendships_distinct_users CHECK (requester_id <> addressee_id),
    CONSTRAINT ck_friendships_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'BLOCKED')),
    CONSTRAINT ck_friendships_response
        CHECK (
            (status = 'PENDING' AND responded_at IS NULL)
            OR (status <> 'PENDING' AND responded_at IS NOT NULL)
        ),
    CONSTRAINT fk_friendships_requester
        FOREIGN KEY (requester_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_friendships_addressee
        FOREIGN KEY (addressee_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE CASCADE,

    INDEX idx_friendships_requester_status (requester_id, status),
    INDEX idx_friendships_addressee_status (addressee_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE conversations (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    type            VARCHAR(20) NOT NULL,
    created_by      BIGINT UNSIGNED NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                                  ON UPDATE CURRENT_TIMESTAMP(3),

    CONSTRAINT pk_conversations PRIMARY KEY (id),
    CONSTRAINT ck_conversations_type CHECK (type IN ('DIRECT', 'GROUP')),
    CONSTRAINT fk_conversations_creator
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE SET NULL,

    INDEX idx_conversations_type_updated (type, updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE conversation_members (
    conversation_id BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    member_role     VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    left_at         DATETIME(3) NULL,
    last_read_at    DATETIME(3) NULL,

    CONSTRAINT pk_conversation_members PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT ck_conversation_members_role
        CHECK (member_role IN ('MEMBER', 'ADMIN', 'OWNER')),
    CONSTRAINT ck_conversation_members_left
        CHECK (left_at IS NULL OR left_at >= joined_at),
    CONSTRAINT fk_conversation_members_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_conversation_members_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE CASCADE,

    INDEX idx_conversation_members_user_active (user_id, left_at, conversation_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE group_details (
    conversation_id BIGINT UNSIGNED NOT NULL,
    owner_id        BIGINT UNSIGNED NOT NULL,
    name            VARCHAR(100) NOT NULL,
    avatar_url      VARCHAR(2048) NULL,
    description     TEXT NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    CONSTRAINT pk_group_details PRIMARY KEY (conversation_id),
    CONSTRAINT fk_group_details_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_group_details_owner
        FOREIGN KEY (owner_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    INDEX idx_group_details_owner (owner_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE messages (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT UNSIGNED NOT NULL,
    sender_id       BIGINT UNSIGNED NULL,
    reply_to_id     BIGINT UNSIGNED NULL,
    content         TEXT NULL,
    message_type    VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    status          VARCHAR(20) NOT NULL DEFAULT 'SENT',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    edited_at       DATETIME(3) NULL,

    CONSTRAINT pk_messages PRIMARY KEY (id),
    CONSTRAINT ck_messages_type
        CHECK (message_type IN ('TEXT', 'FILE', 'IMAGE', 'SYSTEM')),
    CONSTRAINT ck_messages_status
        CHECK (status IN ('SENT', 'EDITED', 'DELETED')),
    CONSTRAINT ck_messages_content
        CHECK (message_type <> 'TEXT' OR content IS NOT NULL),
    CONSTRAINT ck_messages_edited
        CHECK (
            (status = 'EDITED' AND edited_at IS NOT NULL)
            OR status <> 'EDITED'
        ),
    CONSTRAINT fk_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender
        FOREIGN KEY (sender_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_messages_reply
        FOREIGN KEY (reply_to_id) REFERENCES messages (id)
        ON UPDATE CASCADE ON DELETE SET NULL,

    INDEX idx_messages_conversation_time (conversation_id, created_at, id),
    INDEX idx_messages_sender_time (sender_id, created_at),
    INDEX idx_messages_reply (reply_to_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE message_attachments (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    message_id      BIGINT UNSIGNED NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_url        VARCHAR(2048) NOT NULL,
    mime_type       VARCHAR(255) NOT NULL,
    file_size       BIGINT UNSIGNED NOT NULL,

    CONSTRAINT pk_message_attachments PRIMARY KEY (id),
    CONSTRAINT ck_message_attachments_size CHECK (file_size > 0),
    CONSTRAINT fk_message_attachments_message
        FOREIGN KEY (message_id) REFERENCES messages (id)
        ON UPDATE CASCADE ON DELETE CASCADE,

    INDEX idx_message_attachments_message (message_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE message_reads (
    message_id      BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    read_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    CONSTRAINT pk_message_reads PRIMARY KEY (message_id, user_id),
    CONSTRAINT fk_message_reads_message
        FOREIGN KEY (message_id) REFERENCES messages (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_message_reads_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE CASCADE,

    INDEX idx_message_reads_user_time (user_id, read_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE reports (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    reporter_id     BIGINT UNSIGNED NOT NULL,
    message_id      BIGINT UNSIGNED NOT NULL,
    reason          VARCHAR(50) NOT NULL,
    details         TEXT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by     BIGINT UNSIGNED NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    reviewed_at     DATETIME(3) NULL,

    CONSTRAINT pk_reports PRIMARY KEY (id),
    CONSTRAINT uk_reports_reporter_message UNIQUE (reporter_id, message_id),
    CONSTRAINT ck_reports_status
        CHECK (status IN ('PENDING', 'REVIEWED', 'DISMISSED')),
    CONSTRAINT ck_reports_review
        CHECK (
            (status = 'PENDING' AND reviewed_by IS NULL AND reviewed_at IS NULL)
            OR (status <> 'PENDING' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL)
        ),
    CONSTRAINT fk_reports_reporter
        FOREIGN KEY (reporter_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_reports_message
        FOREIGN KEY (message_id) REFERENCES messages (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_reports_reviewer
        FOREIGN KEY (reviewed_by) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    INDEX idx_reports_status_created (status, created_at),
    INDEX idx_reports_message (message_id),
    INDEX idx_reports_reviewer (reviewed_by)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE ai_requests (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL,
    conversation_id BIGINT UNSIGNED NULL,
    prompt          TEXT NOT NULL,
    response        MEDIUMTEXT NULL,
    model           VARCHAR(100) NOT NULL,
    input_tokens    INT UNSIGNED NOT NULL DEFAULT 0,
    output_tokens   INT UNSIGNED NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    CONSTRAINT pk_ai_requests PRIMARY KEY (id),
    CONSTRAINT ck_ai_requests_status
        CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    CONSTRAINT fk_ai_requests_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_ai_requests_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations (id)
        ON UPDATE CASCADE ON DELETE SET NULL,

    INDEX idx_ai_requests_user_time (user_id, created_at),
    INDEX idx_ai_requests_conversation_time (conversation_id, created_at),
    INDEX idx_ai_requests_status_time (status, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
