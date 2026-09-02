-- Combine the separately landed provider datasets into one normalized
-- enrichment relation for downstream event models.
with
    provider_results as (

        select *
        from {{ ref("stg_ticketmaster_price_enrichment") }}

        union all

        select *
        from {{ ref("stg_universe_price_enrichment") }}

    ),

    deduplicated as (

        -- An occurrence normally belongs to only one provider. If historical
        -- source data associates the same event ID with both providers, retain
        -- the most recent extraction result to preserve one row per event_id.
        select *
        from provider_results
        qualify
            row_number() over (
                partition by event_id
                order by extracted_at desc, landed_at desc, source_file desc
            )
            = 1

    )

select *
from deduplicated
