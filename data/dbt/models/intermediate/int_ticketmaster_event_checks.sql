-- Evaluate listing checks once per build.
-- Keep every source record so processing counts can explain each stage.
{{ config(materialized="table") }}

select
    events.*,
    current_date() as checks_date,
    current_timestamp() as checks_evaluated_at,

    coalesce(
        start_at is not null
        and start_date >= current_date()
        and status_code not in ('offsale', 'rescheduled'),
        false
    ) as passes_date_status_checks,

    coalesce(
        not lower(event_name) rlike '(parking|parkeer|parkeren)', false
    ) as passes_parking_check,

    coalesce(
        not lower(event_name) rlike (
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
        ),
        false
    ) as passes_ticket_extras_check

from {{ ref("stg_ticketmaster_events") }} as events
