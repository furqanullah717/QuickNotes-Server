-- Create reminders table
CREATE TABLE reminders (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    note_id CHAR(36) NOT NULL,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    scheduled_at_epoch_millis BIGINT NOT NULL,
    repeat_type VARCHAR(20) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at_epoch_millis BIGINT NOT NULL,
    updated_at_epoch_millis BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_reminders_user_id ON reminders(user_id);
CREATE INDEX idx_reminders_note_id ON reminders(note_id);
CREATE INDEX idx_reminders_user_updated_at ON reminders(user_id, updated_at DESC);
CREATE INDEX idx_reminders_scheduled_at ON reminders(scheduled_at_epoch_millis);
CREATE INDEX idx_reminders_is_deleted ON reminders(is_deleted);

