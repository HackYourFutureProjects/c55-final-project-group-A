ALTER TABLE event_registry
    ALTER COLUMN external_event_key TYPE TEXT;

CREATE OR REPLACE FUNCTION build_stable_key(
    source TEXT,
    source_url TEXT,
    external_event_id TEXT,
    external_venue_id TEXT,
    start_date DATE
)
    RETURNS TEXT
    LANGUAGE sql
    IMMUTABLE
AS
$$
SELECT source
           || '|'
           || COALESCE(
               NULLIF(REGEXP_REPLACE(BTRIM(source_url), '\?.*$', ''), ''),
               'event-id:' || external_event_id
              )
           || '|'
           || COALESCE(external_venue_id, '')
           || '|'
           || start_date::TEXT;
$$;

CREATE OR REPLACE FUNCTION canonical_event_uuid(stable_key TEXT)
    RETURNS UUID
    LANGUAGE sql
    IMMUTABLE
AS
$$
SELECT MD5('hyf-event-v1|' || stable_key)::UUID;
$$;