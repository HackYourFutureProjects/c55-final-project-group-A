-- The backend must receive either a complete valid price range or no price.
select *
from {{ ref("fct_external_events") }}
where
    (is_price_known and (price_min is null or price_max is null or currency is null))
    or (
        not is_price_known
        and (price_min is not null or price_max is not null or currency is not null)
    )
    or price_min > price_max
