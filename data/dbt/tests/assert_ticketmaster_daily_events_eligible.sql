-- Fails when a row violates the reusable rules for backend-facing events.
select *
from {{ ref("int_ticketmaster_daily_events") }}
where
    start_at is null
    or start_date < current_date()
    or status_code in ('offsale', 'rescheduled')
    or lower(event_name) rlike '(parking|parkeer|parkeren)'
    or lower(event_name) rlike (
        'venue premium packages?'
        || '|premium seats'
        || '|vip packages?'
        || '|vinyl room upgrades?'
        || '|vinyl room package'
        || '|ticket not included'
        || '|arrangement strandclub'
        || '|strandclub arrangement'
        || '|comfort seats'
        || '|vip upgrades?'
        || '|accessible tickets'
        || '|rolstoel[[:space:]]*/?[[:space:]]*begeleider'
        || '|after-show meet & greet'
        || '|[|][[:space:]]*vip[[:space:]]*$'
        || '|[|][[:space:]]*sky lounge[[:space:]]*$'
    )
    or occurrence_count < 1
