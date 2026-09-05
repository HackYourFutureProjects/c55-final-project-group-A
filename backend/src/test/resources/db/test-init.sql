CREATE SCHEMA analytics;

CREATE TABLE analytics.external_events (
    source TEXT NOT NULL,
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    external_event_id TEXT NOT NULL,
    source_url TEXT,
    external_venue_id TEXT,
    start_date DATE NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    category TEXT,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ,
    price_min NUMERIC,
    street_name TEXT,
    house_number TEXT,
    postal_code TEXT,
    city_name TEXT,
    province TEXT,
    latitude NUMERIC(9, 6),
    longitude NUMERIC(9, 6),
    image_url TEXT,
    is_cancelled BOOLEAN NOT NULL DEFAULT FALSE
);
