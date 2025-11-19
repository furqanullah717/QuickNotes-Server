-- Create reminders table
CREATE TABLE reminders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    note_id UUID NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    scheduled_at_epoch_millis BIGINT NOT NULL,
    repeat_type VARCHAR(20) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at_epoch_millis BIGINT NOT NULL,
    updated_at_epoch_millis BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX idx_reminders_user_id ON reminders(user_id);
CREATE INDEX idx_reminders_note_id ON reminders(note_id);
CREATE INDEX idx_reminders_user_updated_at ON reminders(user_id, updated_at DESC);
CREATE INDEX idx_reminders_scheduled_at ON reminders(scheduled_at_epoch_millis);
CREATE INDEX idx_reminders_is_deleted ON reminders(is_deleted) WHERE is_deleted = FALSE;

-- Create trigger to automatically update updated_at
CREATE TRIGGER update_reminders_updated_at BEFORE UPDATE ON reminders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

