-- MySQL version: Create account_deletions table
CREATE TABLE account_deletions (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    requested_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    scheduled_deletion_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY unique_user_id (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_account_deletions_user_id ON account_deletions(user_id);
CREATE INDEX idx_account_deletions_scheduled_deletion_at ON account_deletions(scheduled_deletion_at);
CREATE INDEX idx_account_deletions_deleted_at ON account_deletions(deleted_at);

