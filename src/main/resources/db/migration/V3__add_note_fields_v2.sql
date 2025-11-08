-- Add new fields for V2 API
ALTER TABLE notes 
ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS tags TEXT NOT NULL DEFAULT '',
ADD COLUMN IF NOT EXISTS checklist TEXT NOT NULL DEFAULT '',
ADD COLUMN IF NOT EXISTS color_tag TEXT NOT NULL DEFAULT '';

-- Create index for pinned notes
CREATE INDEX IF NOT EXISTS idx_notes_user_pinned ON notes(user_id, is_pinned) WHERE is_pinned = TRUE;

