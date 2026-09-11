-- Backend-facing events with the inferred venue setting.
-- Keep every current event, including events without an enrichment result.
select events.*, coalesce(attributes.venue_setting, 'unknown') as venue_setting
from {{ ref("fct_external_events") }} as events
left join
    {{ ref("fct_event_attributes") }} as attributes
    on events.logical_event_id = attributes.logical_event_id
