-- One row represents the latest Universe GraphQL price result for one external
-- Ticketmaster occurrence ID.
--
-- Ingestion stores one enrichment record per unique source listing and keeps
-- all Ticketmaster occurrence IDs associated with that listing in an array.
-- Staging expands that array so downstream event models can join by event_id.
with
    source as (

        select
            provider,
            listing_key,
            normalized_source_url,
            external_event_ids,
            price_min,
            price_max,
            currency,
            is_price_known,
            age_limit,
            extraction_status,
            extraction_method,
            error_code,
            extracted_at,
            ingest_date,
            _metadata.file_path as source_file,
            _metadata.file_modification_time as landed_at

        from
            read_files(
                '{{ var("universe_price_enrichment_path") }}',
                format => 'json',
                schemahints => '
                    provider STRING,
                    listing_key STRING,
                    normalized_source_url STRING,
                    external_event_ids ARRAY<STRING>,
                    price_min STRING,
                    price_max STRING,
                    currency STRING,
                    is_price_known BOOLEAN,
                    age_limit STRING,
                    extraction_status STRING,
                    extraction_method STRING,
                    error_code STRING,
                    extracted_at STRING
                '
            )

    ),

    exploded as (

        select
            nullif(trim(event_id), '') as event_id,
            nullif(trim(provider), '') as price_provider,
            nullif(trim(listing_key), '') as listing_key,
            nullif(trim(normalized_source_url), '') as normalized_source_url,

            try_cast(price_min as decimal(10, 2)) as price_min,
            try_cast(price_max as decimal(10, 2)) as price_max,
            nullif(trim(currency), '') as currency,
            coalesce(is_price_known, false) as is_price_known,

            nullif(trim(age_limit), '') as age_limit,
            nullif(trim(extraction_status), '') as extraction_status,
            nullif(trim(extraction_method), '') as extraction_method,
            nullif(trim(error_code), '') as error_code,
            try_cast(extracted_at as timestamp) as extracted_at,

            ingest_date,
            source_file,
            landed_at

        from source
        lateral view explode(external_event_ids) exploded_ids as event_id

    ),

    deduplicated as (

        -- Daily landing files may contain newer extraction attempts for the
        -- same occurrence ID. Keep the latest result, including a failed or
        -- unavailable result, so monitoring reflects the current extraction.
        select *
        from exploded
        where event_id is not null
        qualify
            row_number() over (
                partition by event_id
                order by extracted_at desc, landed_at desc, source_file desc
            )
            = 1

    )

select *
from deduplicated
