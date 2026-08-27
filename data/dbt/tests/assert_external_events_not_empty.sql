-- The published mart must contain at least one event. Generic column tests
-- also pass on an empty table, so this assertion protects the pipeline from
-- publishing an empty result.

select 1
where not exists (
    select 1
    from {{ ref("fct_external_events") }}
)