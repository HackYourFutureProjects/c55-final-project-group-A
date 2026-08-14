ALTER TABLE users
    ADD COLUMN role          VARCHAR(20)  NOT NULL DEFAULT 'user',
    ADD COLUMN name          VARCHAR(150) NOT NULL DEFAULT '',
    ADD COLUMN password_hash VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN created_at    TIMESTAMPTZ  NOT NULL DEFAULT now();

ALTER TABLE users
    ADD CONSTRAINT users_email_unique UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT users_role_check CHECK (role IN ('user', 'admin'));

CREATE TABLE sessions
(
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id           UUID         NOT NULL,
    access_token_hash VARCHAR(255) NOT NULL,
    access_created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    access_expires_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT sessions_pk PRIMARY KEY (id),
    CONSTRAINT sessions_user_id_fk FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE

);

CREATE INDEX idx_sessions_access_token_hash ON sessions (access_token_hash);


