-- Return a row when processing metrics are incomplete or inconsistent.
with metrics as (select * from {{ ref("agg_event_processing_metrics") }})

select 'expected_one_metrics_row' as issue
where (select count(*) from metrics) <> 1

union all

select 'invalid_processing_counts' as issue
from metrics
where
    event_set_fingerprint is null
    or source_records is null
    or after_date_status_checks is null
    or after_parking_removed is null
    or after_ticket_extras_removed is null
    or grouped_event_cards is null
    or represented_records is null
    or checks_date is null
    or checks_evaluated_at is null
    or latest_source_ingested_at is null
    or metrics_built_at is null
    or grouped_event_cards <= 0
    or source_records < after_date_status_checks
    or after_date_status_checks < after_parking_removed
    or after_parking_removed < after_ticket_extras_removed
    or after_ticket_extras_removed < grouped_event_cards
    or after_ticket_extras_removed <> represented_records
