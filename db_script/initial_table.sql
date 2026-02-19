CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE followed_user_table (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100),
    picture_url TEXT,
    status VARCHAR(20) DEFAULT 'FOLLOW',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP,
    updated_by VARCHAR(100)
);

CREATE TABLE reminders_table (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sender_id VARCHAR(50) NOT NULL,           -- LINE userId ของคุณ
    target_id VARCHAR(50) NOT NULL,           -- LINE userId ของคนรับ (แฟน)
    message_content TEXT NOT NULL,
    remind_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    snooze_count INT DEFAULT 0,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_remind_at ON reminders_table (remind_at);
CREATE INDEX idx_status ON reminders_table (status);