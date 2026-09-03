CREATE TABLE notifications
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    type        VARCHAR(40)  NOT NULL,
    title       VARCHAR(150) NOT NULL,
    body        TEXT         NOT NULL,
    resource_id UUID         NOT NULL,
    link_path   VARCHAR(500),
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT chk_notifications_type
        CHECK (type IN (
                        'EVENT_CANCELLED',
                        'EVENT_UPDATED',
                        'EVENT_REMINDER',
                        'COMMENT_REPLY',
                        'NEW_FEEDBACK'
            ))
);

CREATE INDEX idx_notifications_user_created
    ON notifications (user_id, created_at DESC, id DESC);

CREATE INDEX idx_notifications_user_unread
    ON notifications (user_id)
    WHERE read_at IS NULL;

CREATE UNIQUE INDEX uq_notifications_once_ever
    ON notifications (user_id, type, resource_id)
    WHERE type IN (
                   'EVENT_CANCELLED',
                   'EVENT_REMINDER',
                   'COMMENT_REPLY',
                   'NEW_FEEDBACK'
        );

CREATE UNIQUE INDEX uq_notifications_event_updated_unread
    ON notifications (user_id, type, resource_id)
    WHERE type = 'EVENT_UPDATED'
        AND read_at IS NULL;

CREATE TABLE notification_outbox
(
    id           UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    type         VARCHAR(40) NOT NULL,
    resource_id  UUID        NOT NULL,
    payload      JSONB       NOT NULL DEFAULT '{}'::JSONB,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,

    CONSTRAINT chk_notification_outbox_type
        CHECK (type IN (
                        'EVENT_CANCELLED',
                        'EVENT_UPDATED',
                        'COMMENT_REPLY',
                        'NEW_FEEDBACK'
            ))
);

CREATE INDEX idx_notification_outbox_pending
    ON notification_outbox (created_at, id)
    WHERE processed_at IS NULL;