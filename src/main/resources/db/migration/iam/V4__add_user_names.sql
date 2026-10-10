ALTER TABLE iam.users ADD COLUMN IF NOT EXISTS first_name VARCHAR(80);
ALTER TABLE iam.users ADD COLUMN IF NOT EXISTS last_name VARCHAR(80);

-- Backfill: accounts created before names existed show the email local part as first name.
UPDATE iam.users SET first_name = split_part(email, '@', 1) WHERE first_name IS NULL;
UPDATE iam.users SET last_name = '' WHERE last_name IS NULL;

ALTER TABLE iam.users ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE iam.users ALTER COLUMN last_name SET NOT NULL;
