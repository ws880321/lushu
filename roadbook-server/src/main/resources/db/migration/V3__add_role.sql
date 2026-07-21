ALTER TABLE users ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'user' COMMENT 'user|editor|admin' AFTER phone;
