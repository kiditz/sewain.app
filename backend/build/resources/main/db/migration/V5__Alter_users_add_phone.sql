ALTER TABLE users
    ADD COLUMN phone VARCHAR(30),
    ADD CONSTRAINT uk_users_phone UNIQUE (phone);