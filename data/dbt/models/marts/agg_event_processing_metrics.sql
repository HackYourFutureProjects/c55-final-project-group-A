-- Processing counts to publish alongside the corresponding events.
{% set metrics_build_id = var("health_metrics_build_id", "") | replace("'", "''") %}

with
    listing_counts as (
        select
            count(*) as source_records,

            count_if(passes_date_status_checks) as after_date_status_checks,

            count_if(
                passes_date_status_checks and passes_parking_check
            ) as after_parking_removed,

            count_if(
                passes_date_status_checks
                and passes_parking_check
                and passes_ticket_extras_check
            ) as after_ticket_extras_removed,

            min(checks_date) as checks_date,
            min(checks_evaluated_at) as checks_evaluated_at,
            max(ingested_at) as latest_source_ingested_at

        from {{ ref("int_ticketmaster_event_checks") }}
    ),

    event_counts as (
        select
            count(*) as grouped_event_cards,
            coalesce(sum(occurrence_count), 0) as represented_records,

            sha2(
                concat_ws(
                    '|',
                    sort_array(
                        collect_list(
                            concat(
                                logical_event_id, ':', cast(occurrence_count as string)
                            )
                        )
                    )
                ),
                256
            ) as event_set_fingerprint

        from {{ ref("fct_external_events") }}
    )

select
    listing_counts.*,
    event_counts.*,
    '{{ metrics_build_id }}' as metrics_build_id,
    current_timestamp() as metrics_built_at
from listing_counts
cross join event_counts
