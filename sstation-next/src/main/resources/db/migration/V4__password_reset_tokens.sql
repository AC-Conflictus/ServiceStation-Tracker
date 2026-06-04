-- Self-service password reset (TC-028 / TC-108g). Only the SHA-256 hash of each token is stored;
-- tokens are single-use and expire after one hour.

CREATE TABLE password_reset_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used       BOOLEAN NOT NULL DEFAULT FALSE,
    -- Drop a user's outstanding reset tokens when the user is deleted.
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_reset_token_hash ON password_reset_tokens (token_hash);
