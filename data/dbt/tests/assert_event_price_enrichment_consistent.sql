-- A known price must be complete, an unknown price must not be partially
-- populated, and every range must be ordered from minimum to maximum.
select *
from {{ ref("int_event_price_enrichment") }}
where
    (is_price_known and (price_min is null or price_max is null or currency is null))
    or (
        not is_price_known
        and (price_min is not null or price_max is not null or currency is not null)
    )
    or price_min > price_max
