-- Fails when a row violates the reusable rules for backend-facing events.
select *
from {{ ref("int_ticketmaster_daily_events") }}
where
    start_at is null
    or start_date < current_date()
    or status_code in ('offsale', 'rescheduled')
    or lower(event_name) rlike '(parking|parkeer|parkeren)'
    or occurrence_count < 1
