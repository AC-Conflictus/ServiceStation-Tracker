-- Ordinary events retain direct signup; admins explicitly enable site forms per occurrence.
ALTER TABLE events ADD COLUMN registration_mode VARCHAR(20) NOT NULL DEFAULT 'DIRECT';
