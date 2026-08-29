CREATE TABLE feedback (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic VARCHAR(10) NOT NULL CHECK (topic IN ('app', 'event')),
    event_title VARCHAR(255),
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    message TEXT,
    sender_name VARCHAR(150),
    sender_email VARCHAR(255),
    is_reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);