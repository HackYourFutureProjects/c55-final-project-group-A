CREATE INDEX idx_event_attendees_event_id
    ON event_attendees (event_id);

CREATE INDEX idx_saved_events_event_id
    ON saved_events (event_id);