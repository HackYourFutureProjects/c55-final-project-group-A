CREATE OR REPLACE VIEW event_feed AS
SELECT e.id,
       e.title,
       e.description,
       ARRAY(
               SELECT c.id
               FROM event_categories ec
                        JOIN categories c ON c.id = ec.category_id
               WHERE ec.event_id = e.id
               ORDER BY c.name
       )                          AS category_ids,
       ARRAY(
               SELECT c.name
               FROM event_categories ec
                        JOIN categories c ON c.id = ec.category_id
               WHERE ec.event_id = e.id
               ORDER BY c.name
       )                          AS category_names,
       e.start_at,
       e.end_at,
       e.price,
       a.street,
       a.house_number,
       a.postal_code,
       a.city_name,
       a.province,
       a.latitude,
       a.longitude,
       (SELECT ei.image_url
        FROM event_images ei
        WHERE ei.event_id = e.id
        ORDER BY ei.created_at, ei.id
        LIMIT 1)                  AS image_url,
       (SELECT COUNT(*)::BIGINT
        FROM event_attendees ea
        WHERE ea.event_id = e.id) AS going_count,
       (
           3 * (SELECT COUNT(*)
                FROM event_attendees ea
                WHERE ea.event_id = e.id)
               + (SELECT COUNT(*)
                  FROM saved_events se
                  WHERE se.event_id = e.id)
           )                      AS popularity_score,
       e.is_cancelled,
       e.is_published,
       'app'::TEXT                AS source,
       NULL::TEXT                 AS stable_event_key,
       NULL::TEXT                 AS source_url
FROM events e
         JOIN addresses a ON a.id = e.address_id

UNION ALL

SELECT ext.event_id,
       ext.title,
       ext.description,
       CASE
           WHEN matched.id IS NOT NULL THEN ARRAY [matched.id]
           ELSE ARRAY [fallback.id]
           END                            AS category_ids,
       CASE
           WHEN matched.name IS NOT NULL THEN ARRAY [matched.name]
           ELSE ARRAY ['Other']
           END                            AS category_names,
       ext.start_at,
       ext.end_at,
       ext.price_min                      AS price,
       ext.street_name                    AS street,
       ext.house_number,
       ext.postal_code,
       ext.city_name,
       ext.province,
       ext.latitude::NUMERIC(9, 6)        AS latitude,
       ext.longitude::NUMERIC(9, 6)       AS longitude,
       ext.image_url,
       (SELECT COUNT(*)::BIGINT
        FROM event_attendees ea
        WHERE ea.event_id = ext.event_id) AS going_count,
       (
           3 * (SELECT COUNT(*)
                FROM event_attendees ea
                WHERE ea.event_id = ext.event_id)
               + (SELECT COUNT(*)
                  FROM saved_events se
                  WHERE se.event_id = ext.event_id)
           )                              AS popularity_score,
       ext.is_cancelled,
    
       ext.is_published                   AS is_published,
       ext.source,
       ext.stable_event_key,
       ext.source_url
FROM (SELECT mart.*,
             build_stable_key(
                     mart.source,
                     mart.source_url,
                     mart.external_event_id,
                     mart.external_venue_id,
                     mart.start_date
             ) AS stable_event_key,
             canonical_event_uuid(
                     build_stable_key(
                             mart.source,
                             mart.source_url,
                             mart.external_event_id,
                             mart.external_venue_id,
                             mart.start_date
                     )
             ) AS event_id
      FROM analytics.external_events mart) ext
         LEFT JOIN categories matched ON matched.name = ext.category
         CROSS JOIN categories fallback
WHERE fallback.name = 'Other';