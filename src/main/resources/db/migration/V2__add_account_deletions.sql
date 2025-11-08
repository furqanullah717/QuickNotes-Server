-- Create account_deletions table
CREATE TABLE account_deletions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    scheduled_deletion_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id)
);

-- Create indexes for performance
CREATE INDEX idx_account_deletions_user_id ON account_deletions(user_id);
CREATE INDEX idx_account_deletions_scheduled_deletion_at ON account_deletions(scheduled_deletion_at);
CREATE INDEX idx_account_deletions_deleted_at ON account_deletions(deleted_at) WHERE deleted_at IS NULL;

