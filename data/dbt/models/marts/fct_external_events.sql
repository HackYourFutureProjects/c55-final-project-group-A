-- This mart is the contract with the backend team.
--
-- One row represents one logical Ticketmaster event per venue and local date,
-- represented by its earliest eligible occurrence.
-- Its columns are what the backend receives after the publishing step, so
-- treat renaming, removing or changing the type of a column like changing a
-- public API: agree it with the backend team first.
--
-- After dbt succeeds, the publishing step copies this table into the
-- PostgreSQL analytics schema. Whatever this model selects is what the
-- backend can consume.
--
-- Reusable eligibility rules, parking exclusion, address parsing and daily
-- occurrence grouping are applied in int_ticketmaster_daily_events. This mart
-- maps those logical events to the backend-facing contract.
with
    ticketmaster_daily_events as (
        select * from {{ ref("int_ticketmaster_daily_events") }}
    ),

    normalized as (

        select
            -- The earliest occurrence keeps its original Ticketmaster ID. Prefixing
            -- prevents collisions if another external source uses the same ID, while
            -- the backend maintains its own internal UUID.
            concat(source, ':', event_id) as external_event_key,
            event_id as external_event_id,
            source,

            event_name as title,
            event_info as description,
            event_url as source_url,

            -- Draft mapping from Ticketmaster classifications to the current
            -- backend category vocabulary.
            case
                when genre_name = 'Family'
                then 'Family & Kids'
                when genre_name = 'Food & Drink'
                then 'Food & Drink'
                when genre_name = 'Community/Civic'
                then 'Community & Social'
                when
                    genre_name
                    in ('Theatre', 'Comedy', 'Dance', 'Performance Art', 'Variety')
                then 'Theatre & Performance'
                when genre_name = 'Fine Art'
                then 'Arts & Culture'
                when segment_name = 'Music'
                then 'Music'
                when segment_name = 'Sports'
                then 'Sports & Fitness'
                when segment_name in ('Film', 'Arts & Theatre')
                then 'Arts & Culture'
                else 'Other'
            end as category,

            -- Preserve the source classification alongside our normalized
            -- category so the original meaning is not lost.
            segment_name as source_segment,
            genre_name as source_genre,
            subgenre_name as source_subgenre,
            is_family,

            start_date,
            start_time,
            start_at,
            end_at,
            timezone,
            occurrence_count,
            status_code,
            status_code = 'cancelled' as is_cancelled,
            date_tbd,
            date_tba,
            time_tba,
            no_specific_time,

            -- NULL means Ticketmaster did not supply a price. It does not mean
            -- that the event is free.
            price_min,
            price_max,
            currency,
            price_min is not null as is_price_known,

            venue_id as external_venue_id,
            venue_name,
            address_line1,
            street_name,
            house_number,
            postal_code,
            city_name,
            province,
            country_code,
            latitude,
            longitude,

            image_url,

            -- Operational lineage fields used for freshness and debugging.
            ingest_date,
            ingested_at

        from ticketmaster_daily_events

    )

select *
from normalized
