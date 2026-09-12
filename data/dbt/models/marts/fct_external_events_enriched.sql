-- Backend-facing events with an optional inferred venue setting.
-- The fallback does not require the enrichment table to exist.
{% if var("venue_enrichment_available", true) %}

    select events.*, coalesce(attributes.venue_setting, 'unknown') as venue_setting
    from {{ ref("fct_external_events") }} as events
    left join
        {{ ref("fct_event_attributes") }} as attributes
        on events.logical_event_id = attributes.logical_event_id

{% else %}

    select events.*, cast('unknown' as string) as venue_setting
    from {{ ref("fct_external_events") }} as events

{% endif %}
