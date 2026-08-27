-- Staging does one job: read the raw files and clean them. No business logic.
--
-- `read_files` reads every file in the landing volume, so a new day's file is
-- picked up without you changing anything here. `_metadata.file_path` tells you
-- which file a row came from, which is the first thing you want when one day
-- looks wrong.
--
-- This model is a table, not a view, and that is a deliberate choice. A view
-- would re-read every file in the landing folder for each model and each test
-- that selects from it, which is more than a dozen full reads per `dbt build`.
-- As a table the files are read once and everything downstream reads the
-- result. See dbt_project.yml.
with
    source as (

        -- Databricks infers the nested Ticketmaster schema from all files in
        -- the landing folder. The current development batch contains no price
        -- ranges or please-note values, so those fields would otherwise be
        -- absent from the inferred schema. These hints keep them available as
        -- nullable fields and allow future files to provide real values.
        select
            *,
            _metadata.file_path as source_file,
            _metadata.file_modification_time as ingested_at
        from
            -- You do not need a raw table. `read_files` reads the JSON straight
            -- out of the landing folder, so there is no CREATE TABLE step to
            -- write and nothing to keep in sync: this staging model is the
            -- first thing that touches the data.
            --
            -- It handles a folder whose files do not all have the same shape:
            -- it infers one unified schema across every file it reads. A field
            -- only present in newer files is simply empty for the older rows
            -- rather than failing the read, so a source that adds a field next
            -- month needs no backfill and no change here.
            --
            -- https://docs.databricks.com/aws/en/sql/language-manual/functions/read_files
            read_files(
                '{{ var("landing_path") }}',
                format => 'json',
                schemahints
                => '
                    priceRanges ARRAY<STRUCT<
                        type: STRING,
                        currency: STRING,
                        min: DOUBLE,
                        max: DOUBLE
                    >>,
                    pleaseNote STRING
                '
            )

    ),

    extracted as (

        select
            *,

            -- Prefer the classification marked as primary. Some Ticketmaster
            -- events do not mark one, so fall back to the first classification.
            coalesce(
                try_element_at(
                    filter(
                        classifications,
                        classification -> classification.primary = true
                    ),
                    1
                ),
                try_element_at(classifications, 1)
            ) as selected_classification,

            -- Ticketmaster stores these values in arrays. For the initial mart,
            -- use one venue, one image and one price range per event.
            try_element_at(_embedded.venues, 1) as selected_venue,
            try_element_at(images, 1) as selected_image,
            try_element_at(priceRanges, 1) as selected_price_range

        from source

    ),

    renamed as (

        select
            -- Rename source fields to stable project names and cast values
            -- into types that downstream dbt models can rely on.
            trim(id) as event_id,
            trim(name) as event_name,

            coalesce(
                nullif(trim(info), ''),
                nullif(trim(pleaseNote), '')
            ) as event_info,

            nullif(trim(url), '') as event_url,

            try_cast(dates.start.localDate as date) as start_date,
            nullif(trim(dates.start.localTime), '') as start_time,
            try_cast(dates.start.dateTime as timestamp) as start_at,
            try_cast(dates.end.dateTime as timestamp) as end_at,
            nullif(trim(dates.timezone), '') as timezone,
            nullif(trim(dates.status.code), '') as status_code,

            coalesce(dates.start.dateTBD, false) as date_tbd,
            coalesce(dates.start.dateTBA, false) as date_tba,
            coalesce(dates.start.timeTBA, false) as time_tba,
            coalesce(dates.start.noSpecificTime, false) as no_specific_time,
            coalesce(dates.spanMultipleDays, false) as spans_multiple_days,

            selected_classification.segment.id as segment_id,
            selected_classification.segment.name as segment_name,
            selected_classification.genre.id as genre_id,
            selected_classification.genre.name as genre_name,
            selected_classification.subGenre.id as subgenre_id,
            selected_classification.subGenre.name as subgenre_name,
            selected_classification.type.name as classification_type,
            selected_classification.subType.name as classification_subtype,
            coalesce(selected_classification.family, false) as is_family,

            selected_venue.id as venue_id,
            nullif(trim(selected_venue.name), '') as venue_name,
            nullif(trim(selected_venue.address.line1), '') as address_line1,
            nullif(trim(selected_venue.postalCode), '') as postal_code,
            nullif(trim(selected_venue.city.name), '') as city_name,

            -- State is absent from the current Netherlands dataset. Reading it
            -- through JSON allows the model to return NULL now and pick it up
            -- if a future response contains it.
            nullif(
                trim(
                    get_json_object(
                        to_json(selected_venue),
                        '$.state.name'
                    )
                ),
                ''
            ) as province,

            nullif(
                trim(selected_venue.country.countryCode),
                ''
            ) as country_code,

            try_cast(
                selected_venue.location.latitude as decimal(9, 6)
            ) as latitude,

            try_cast(
                selected_venue.location.longitude as decimal(9, 6)
            ) as longitude,

            nullif(trim(selected_image.url), '') as image_url,

            try_cast(
                selected_price_range.min as decimal(10, 2)
            ) as price_min,

            try_cast(
                selected_price_range.max as decimal(10, 2)
            ) as price_max,

            nullif(
                trim(selected_price_range.currency),
                ''
            ) as currency,

            'ticketmaster' as source,
            source_file,
            ingest_date,
            ingested_at

        from extracted

    ),

    deduplicated as (

        -- read_files scans every ingestion date in the landing folder. The
        -- same Ticketmaster event can therefore appear in multiple daily
        -- files. Keep the most recently ingested version so staging exposes
        -- exactly one current row per external event.
        select *
        from renamed
        qualify
            row_number() over (
                partition by event_id
                order by ingested_at desc, source_file desc
                )
                = 1

    )

select *
from deduplicated
