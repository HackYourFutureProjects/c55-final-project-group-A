CREATE TABLE event_categories
(
    event_id    UUID NOT NULL,
    category_id UUID NOT NULL,

    CONSTRAINT pk_event_categories
        PRIMARY KEY (event_id, category_id),

    CONSTRAINT fk_event_categories_event
        FOREIGN KEY (event_id)
            REFERENCES events (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_event_categories_category
        FOREIGN KEY (category_id)
            REFERENCES categories (id)
);

CREATE INDEX idx_event_categories_category_id
    ON event_categories (category_id);

INSERT INTO event_categories (event_id, category_id)
SELECT id, category_id
FROM events
ON CONFLICT DO NOTHING;