-- One row will represent one logical Ticketmaster event per venue and day.
--
-- This intermediate model applies reusable business rules before the final
-- backend contract is created. It excludes records that cannot currently be
-- consumed by the backend and collapses multiple booking slots into one
-- logical daily event.
with
    ticketmaster_events as (select * from {{ ref("stg_ticketmaster_events") }}),
    price_enrichment as (select * from {{ ref("stg_event_price_enrichment") }}),

    eligible_events as (

        -- The initial backend contract requires an exact start timestamp.
        -- Events from previous calendar days and statuses that should not be
        -- displayed are excluded. Cancelled and postponed events remain so the
        -- backend can handle them explicitly.
        select *
        from ticketmaster_events
        where
            start_at is not null
            and start_date >= current_date()
            and status_code not in ('offsale', 'rescheduled')

    ),

    non_parking_events as (

        -- Ticketmaster may expose parking permits as events with their own
        -- event IDs. They are purchasable products, but they are not events
        -- that should appear in the backend event catalogue.
        select *
        from eligible_events
        where not lower(event_name) rlike '(parking|parkeer|parkeren)'

    ),

    parsed_addresses as (

        select
            *,

            -- If the address matches the common Dutch pattern
            -- `<street> <house number>`, keep only the street part. When the
            -- pattern cannot be recognized reliably, preserve the complete
            -- source address as the street rather than guessing.
            coalesce(
                nullif(
                    trim(
                        regexp_extract(
                            trim(address_line1),
                            '^(.+?)[[:space:]]+([0-9]+(?:[[:space:]]*[A-Za-z])?(?:[-/][0-9A-Za-z-]+)?)(?:,.*)?$',
                            1
                        )
                    ),
                    ''
                ),
                nullif(trim(address_line1), '')
            ) as street_name,

            -- House numbers remain strings because Dutch addresses may include
            -- letters, ranges or separators. NULL means the value could not be
            -- extracted confidently.
            nullif(
                regexp_replace(
                    regexp_extract(
                        trim(address_line1),
                        '^(.+?)[[:space:]]+([0-9]+(?:[[:space:]]*[A-Za-z])?(?:[-/][0-9A-Za-z-]+)?)(?:,.*)?$',
                        2
                    ),
                    '[[:space:]]',
                    ''
                ),
                ''
            ) as house_number

        from non_parking_events

    ),

    grouping_candidates as (

        select
            *,

            -- Query parameters such as `ref=ticketmaster` identify the referral
            -- channel, not a different logical event.
            regexp_replace(event_url, '[?].*$', '') as normalized_event_url

        from parsed_addresses

    ),

    daily_events as (

        -- Ticketmaster may expose each booking slot as a separate event ID.
        -- The backend contract uses one logical event per source page, venue
        -- and local date, represented by the earliest available slot.
        select
            *,

            -- Preserve how many source occurrences were represented by the
            -- selected daily event. The individual occurrence rows remain
            -- available in staging.
            count(*) over (
                partition by
                    source,
                    coalesce(normalized_event_url, concat('event-id:', event_id)),
                    coalesce(venue_id, ''),
                    start_date
            ) as occurrence_count

        from grouping_candidates
        qualify
            row_number() over (
                partition by
                    source,
                    coalesce(normalized_event_url, concat('event-id:', event_id)),
                    coalesce(venue_id, ''),
                    start_date
                order by start_at, event_id
            )
            = 1

    ),

    price_enriched_daily_events as (

        -- Join after daily grouping: every source occurrence ID has already
        -- received its listing price in staging, so the selected earliest
        -- occurrence can be enriched directly by event_id.
        select
            daily_events.*,

            price_enrichment.price_min as enriched_price_min,
            price_enrichment.price_max as enriched_price_max,
            price_enrichment.currency as enriched_currency,
            coalesce(price_enrichment.is_price_known, false) as is_enriched_price_known,

            price_enrichment.price_provider,
            price_enrichment.age_limit as provider_age_limit,
            price_enrichment.extraction_status as price_extraction_status,
            price_enrichment.extraction_method as price_extraction_method,
            price_enrichment.error_code as price_error_code,
            price_enrichment.extracted_at as price_extracted_at

        from daily_events
        left join price_enrichment using (event_id)

    )

select *
from price_enriched_daily_events
