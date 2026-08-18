ALTER TABLE events
    DROP CONSTRAINT fk_events_category,
    DROP COLUMN category_id;