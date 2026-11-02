ALTER TABLE media ADD COLUMN background_removed BOOLEAN NOT NULL DEFAULT FALSE;

-- Files created by the existing upload processor use this suffix.
UPDATE media
SET background_removed = TRUE
WHERE lower(filename) LIKE '%-transparent.png';
