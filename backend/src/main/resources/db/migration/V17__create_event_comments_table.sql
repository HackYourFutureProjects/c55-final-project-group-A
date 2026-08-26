CREATE TABLE event_comments
(
    id                     UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    event_id               UUID         NOT NULL,
    user_id                UUID         NOT NULL,
    content                VARCHAR(500) NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),

    admin_reply            VARCHAR(500),
    admin_reply_by_user_id UUID,
    admin_reply_created_at TIMESTAMPTZ,
    admin_reply_updated_at TIMESTAMPTZ,

    CONSTRAINT fk_event_comments_event
        FOREIGN KEY (event_id)
            REFERENCES events (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_event_comments_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_event_comments_admin
        FOREIGN KEY (admin_reply_by_user_id)
            REFERENCES users (id)
            ON DELETE SET NULL,

    CONSTRAINT chk_event_comments_content_not_blank
        CHECK (length(btrim(content)) > 0),

    CONSTRAINT chk_event_comments_admin_reply_not_blank
        CHECK (
            admin_reply IS NULL
                OR length(btrim(admin_reply)) > 0
            )
);

CREATE INDEX idx_event_comments_event_created_at
    ON event_comments (event_id, created_at DESC);
