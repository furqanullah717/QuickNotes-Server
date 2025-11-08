-- MySQL version: Add new fields for V2 API
-- Note: MySQL doesn't support IF NOT EXISTS for ALTER TABLE, so these will fail silently if columns already exist
-- Note: MySQL doesn't allow DEFAULT values on TEXT columns, so we:
-- 1. Add columns as nullable first
-- 2. Update existing rows to have empty strings
-- 3. Make them NOT NULL

-- Add columns as nullable
ALTER TABLE notes 
ADD COLUMN is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN tags TEXT NULL,
ADD COLUMN checklist TEXT NULL,
ADD COLUMN color_tag TEXT NULL;

-- Update existing rows to have empty strings
UPDATE notes SET tags = '' WHERE tags IS NULL;
UPDATE notes SET checklist = '' WHERE checklist IS NULL;
UPDATE notes SET color_tag = '' WHERE color_tag IS NULL;

-- Make columns NOT NULL
ALTER TABLE notes 
MODIFY COLUMN tags TEXT NOT NULL,
MODIFY COLUMN checklist TEXT NOT NULL,
MODIFY COLUMN color_tag TEXT NOT NULL;

-- Create index for pinned notes (if it doesn't exist)
CREATE INDEX IF NOT EXISTS idx_notes_user_pinned ON notes(user_id, is_pinned);

