CREATE TABLE event_registry
(
    id                 UUID PRIMARY KEY,
    source             VARCHAR(20) NOT NULL CHECK (source IN ('app', 'ticketmaster')),
    internal_event_id  UUID UNIQUE REFERENCES events (id) ON DELETE CASCADE,
    external_event_key VARCHAR(255) UNIQUE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_registry_source_consistency
        CHECK (
            (source = 'app' AND internal_event_id IS NOT NULL AND external_event_key IS NULL) OR
            (source = 'ticketmaster' AND internal_event_id IS NULL AND external_event_key IS NOT NULL)
            )
);

INSERT INTO event_registry (id, source, internal_event_id)
SELECT id, 'app', id
FROM events;

ALTER TABLE saved_events
    DROP CONSTRAINT fk_saved_events_event;
ALTER TABLE saved_events
    ADD CONSTRAINT fk_saved_events_event
        FOREIGN KEY (event_id) REFERENCES event_registry (id) ON DELETE CASCADE;

ALTER TABLE event_attendees
    DROP CONSTRAINT fk_event_attendees_event;
ALTER TABLE event_attendees
    ADD CONSTRAINT fk_event_attendees_event
        FOREIGN KEY (event_id) REFERENCES event_registry (id) ON DELETE CASCADE;

ALTER TABLE event_comments
    DROP CONSTRAINT fk_event_comments_event;
ALTER TABLE event_comments
    ADD CONSTRAINT fk_event_comments_event
        FOREIGN KEY (event_id) REFERENCES event_registry (id) ON DELETE CASCADE;
