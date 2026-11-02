CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(32)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT users_username_uq UNIQUE (username),
    CONSTRAINT users_username_format_ck CHECK (username ~ '^[a-z0-9._-]{3,32}$'),
    CONSTRAINT users_password_hash_format_ck CHECK (password_hash LIKE '{argon2}$argon2id$%')
);

CREATE TABLE posts (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    author_id  BIGINT        NOT NULL REFERENCES users (id),
    content    VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL,
    CONSTRAINT posts_content_not_blank_ck CHECK (btrim(content) <> '')
);

CREATE INDEX posts_author_id_idx ON posts (author_id);

GRANT USAGE ON SCHEMA lab1 TO lab1_rw;
GRANT SELECT, INSERT ON users TO lab1_rw;
GRANT SELECT, INSERT ON posts TO lab1_rw;
