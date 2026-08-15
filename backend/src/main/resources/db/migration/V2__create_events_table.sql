CREATE TYPE message_status AS ENUM (
    'NEW',
    'READ'
    );


CREATE TABLE cities (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(), -- automatically creates a unique UUID when a row is inserted without an ID
                        name VARCHAR(100) NOT NULL,
                        province VARCHAR(100) NOT NULL,

                        CONSTRAINT uq_cities_name_province
                            UNIQUE (name, province)
);


CREATE TABLE categories (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            name VARCHAR(100) NOT NULL,

                            CONSTRAINT uq_categories_name
                                UNIQUE (name)
);


CREATE TABLE addresses (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           street VARCHAR(255) NOT NULL,
                           house_number VARCHAR(20) NOT NULL,
                           postal_code VARCHAR(10) NOT NULL,
                           city_id UUID NOT NULL,
                           latitude NUMERIC(9, 6),
                           longitude NUMERIC(9, 6),

                           CONSTRAINT fk_addresses_city
                               FOREIGN KEY (city_id)
                                   REFERENCES cities (id),

                           CONSTRAINT chk_addresses_latitude
                               CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),

                           CONSTRAINT chk_addresses_longitude
                               CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);


CREATE TABLE events (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        title VARCHAR(255) NOT NULL,
                        description TEXT,
                        category_id UUID NOT NULL,
                        address_id UUID NOT NULL,
                        start_at TIMESTAMPTZ NOT NULL,
                        end_at TIMESTAMPTZ NOT NULL,
                        price NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
                        created_by_user_id UUID NOT NULL,
                        is_cancelled BOOLEAN NOT NULL DEFAULT FALSE,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                        CONSTRAINT fk_events_category
                            FOREIGN KEY (category_id)
                                REFERENCES categories (id),

                        CONSTRAINT fk_events_address
                            FOREIGN KEY (address_id)
                                REFERENCES addresses (id),

                        CONSTRAINT fk_events_created_by_user
                            FOREIGN KEY (created_by_user_id)
                                REFERENCES users (id),

                        CONSTRAINT chk_events_valid_dates
                            CHECK (end_at > start_at),

                        CONSTRAINT chk_events_non_negative_price
                            CHECK (price >= 0)
);


CREATE TABLE event_images (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              event_id UUID NOT NULL,
                              image_key TEXT NOT NULL,
                              content_type VARCHAR(100) NOT NULL,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                              CONSTRAINT fk_event_images_event
                                  FOREIGN KEY (event_id)
                                      REFERENCES events (id)
                                      ON DELETE CASCADE
);


CREATE TABLE saved_events (
                              user_id UUID NOT NULL,
                              event_id UUID NOT NULL,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                              CONSTRAINT pk_saved_events
                                  PRIMARY KEY (user_id, event_id),

                              CONSTRAINT fk_saved_events_user
                                  FOREIGN KEY (user_id)
                                      REFERENCES users (id)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_saved_events_event
                                  FOREIGN KEY (event_id)
                                      REFERENCES events (id)
                                      ON DELETE CASCADE
);


CREATE TABLE event_attendees (
                                 user_id UUID NOT NULL,
                                 event_id UUID NOT NULL,
                                 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                                 CONSTRAINT pk_event_attendees
                                     PRIMARY KEY (user_id, event_id),

                                 CONSTRAINT fk_event_attendees_user
                                     FOREIGN KEY (user_id)
                                         REFERENCES users (id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT fk_event_attendees_event
                                     FOREIGN KEY (event_id)
                                         REFERENCES events (id)
                                         ON DELETE CASCADE
);


CREATE TABLE event_messages (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                event_id UUID NOT NULL,
                                sender_user_id UUID NOT NULL,
                                subject VARCHAR(255),
                                message TEXT NOT NULL,
                                status message_status NOT NULL DEFAULT 'NEW',
                                created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                read_at TIMESTAMPTZ,

                                CONSTRAINT fk_event_messages_event
                                    FOREIGN KEY (event_id)
                                        REFERENCES events (id)
                                        ON DELETE CASCADE,

                                CONSTRAINT fk_event_messages_sender
                                    FOREIGN KEY (sender_user_id)
                                        REFERENCES users (id)
                                        ON DELETE CASCADE
);



CREATE INDEX idx_addresses_city_id
    ON addresses (city_id);


CREATE INDEX idx_events_address_id
    ON events (address_id);

CREATE INDEX idx_events_created_by_user_id
    ON events (created_by_user_id);


CREATE INDEX idx_events_active_start_at
    ON events (start_at)
    WHERE is_cancelled = FALSE;

CREATE INDEX idx_events_end_at
    ON events (end_at);

CREATE INDEX idx_events_price
    ON events (price);

CREATE INDEX idx_event_images_event_id
    ON event_images (event_id);