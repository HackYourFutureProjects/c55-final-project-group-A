-- The data-owned logical event ID must remain compatible with the UUIDs
-- already used by the backend. Any returned row fails the test.
with
    identity_inputs as (

        select
            logical_event_id,
            external_event_id,
            source,
            source_url,
            external_venue_id,
            start_date,

            concat(
                source,
                '|',
                coalesce(
                    nullif(substring_index(trim(source_url), '?', 1), ''),
                    concat('event-id:', external_event_id)
                ),
                '|',
                coalesce(external_venue_id, ''),
                '|',
                cast(start_date as string)
            ) as logical_event_identity_input

        from {{ ref("fct_external_events") }}

    )

select logical_event_id, external_event_id, source_url, external_venue_id, start_date
from identity_inputs
where
    replace(lower(logical_event_id), '-', '')
    is distinct from md5(concat('hyf-event-v1|', logical_event_identity_input))
