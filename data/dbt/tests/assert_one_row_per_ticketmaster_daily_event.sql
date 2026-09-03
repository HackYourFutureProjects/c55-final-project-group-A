-- Fails when more than one row represents the same normalized event title,
-- venue and local date.
select
    source,
    coalesce(
        nullif(lower(trim(event_name)), ''),
        normalized_event_url,
        concat('event-id:', event_id)
    ) as event_group,
    coalesce(venue_id, '') as venue_group,
    start_date,
    count(*) as row_count
from {{ ref("int_ticketmaster_daily_events") }}
group by
    source,
    coalesce(
        nullif(lower(trim(event_name)), ''),
        normalized_event_url,
        concat('event-id:', event_id)
    ),
    coalesce(venue_id, ''),
    start_date
having count(*) > 1
