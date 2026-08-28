package nl.hackyourfuture.project.backend.event.similarity.repository;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.category.model.Category;
import nl.hackyourfuture.project.backend.event.model.EventSummary;
import nl.hackyourfuture.project.backend.event.similarity.model.SimilarEventCandidate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EventSimilarityRepository {

    private final JdbcClient jdbcClient;


    private static List<Category> mapCategories(
            ResultSet rs
    ) throws SQLException {
        UUID[] categoryIds =
                (UUID[]) rs
                        .getArray("category_ids")
                        .getArray();

        String[] categoryNames =
                (String[]) rs
                        .getArray("category_names")
                        .getArray();

        List<Category> categories =
                new ArrayList<>(categoryIds.length);

        for (int index = 0; index < categoryIds.length; index++) {
            categories.add(new Category(
                    categoryIds[index],
                    categoryNames[index]
            ));
        }

        return List.copyOf(categories);
    }

    private static final RowMapper<SimilarEventCandidate>
            SIMILAR_EVENT_ROW_MAPPER =
            (rs, _) -> {
                EventSummary event = new EventSummary(
                        rs.getObject("id", UUID.class),
                        rs.getString("title"),
                        mapCategories(rs),
                        rs.getObject(
                                "start_at",
                                OffsetDateTime.class
                        ),
                        rs.getObject(
                                "end_at",
                                OffsetDateTime.class
                        ),
                        rs.getBigDecimal("price"),
                        rs.getString("street"),
                        rs.getString("house_number"),
                        rs.getString("postal_code"),
                        rs.getString("city_name"),
                        rs.getString("province"),
                        rs.getBigDecimal("latitude"),
                        rs.getBigDecimal("longitude"),
                        rs.getString("image_url"),
                        rs.getLong("going_count"),
                        rs.getBoolean("is_cancelled")
                );

                return new SimilarEventCandidate(
                        event,
                        rs.getDouble("similarity_score")
                );
            };


    public boolean existsPublishedEvent(UUID eventId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM events
                    WHERE id = :eventId
                      AND is_published = TRUE
                )
                """;

        return jdbcClient
                .sql(sql)
                .param("eventId", eventId)
                .query(Boolean.class)
                .single();
    }

    private static final String SOURCE_EVENT_CTE = """
            source_event AS (
                SELECT e.id,
                       a.city_name,
                       e.start_at,
                       e.price,
                       ARRAY(
                           SELECT ec.category_id
                           FROM event_categories ec
                           WHERE ec.event_id = e.id
                           ORDER BY ec.category_id
                       ) AS category_ids,
                       CASE
                           WHEN (
                               e.start_at AT TIME ZONE 'Europe/Amsterdam'
                           )::time >= TIME '06:00'
                           AND (
                               e.start_at AT TIME ZONE 'Europe/Amsterdam'
                           )::time < TIME '12:00'
                               THEN 'MORNING'
                           WHEN (
                               e.start_at AT TIME ZONE 'Europe/Amsterdam'
                           )::time >= TIME '12:00'
                           AND (
                               e.start_at AT TIME ZONE 'Europe/Amsterdam'
                           )::time < TIME '18:00'
                               THEN 'AFTERNOON'
                           ELSE 'EVENING'
                       END AS time_bucket ,
                       EXTRACT(
                           ISODOW FROM
                           e.start_at AT TIME ZONE 'Europe/Amsterdam'
                       ) AS weekday
                FROM events e
                JOIN addresses a ON a.id = e.address_id
                WHERE e.id = :eventId
                  AND e.is_published = TRUE
            )
            """;

    private static final String ELIGIBLE_CANDIDATES_CTE = """
            eligible_candidates AS (
                SELECT e.id AS candidate_id,
                       a.city_name,
                       e.start_at,
                       e.price,
                       ARRAY(
                           SELECT ec.category_id
                           FROM event_categories ec
                           WHERE ec.event_id = e.id
                           ORDER BY ec.category_id
                       ) AS candidate_category_ids,
                       attendee_stats.going_count,
                       (
                           3 * attendee_stats.going_count
                           + saved_stats.saved_count
                       ) AS popularity_score,
                       CASE
                           WHEN (
                               e.start_at AT TIME ZONE 'Europe/Amsterdam'
                           )::time >= TIME '06:00'
                           AND (
                               e.start_at AT TIME ZONE 'Europe/Amsterdam'
                           )::time < TIME '12:00'
                               THEN 'MORNING'
                           WHEN (
                               e.start_at AT TIME ZONE 'Europe/Amsterdam'
                           )::time >= TIME '12:00'
                           AND (
                               e.start_at AT TIME ZONE 'Europe/Amsterdam'
                           )::time < TIME '18:00'
                               THEN 'AFTERNOON'
                           ELSE 'EVENING'
                       END AS time_bucket,
                       EXTRACT(
                           ISODOW FROM
                           e.start_at AT TIME ZONE 'Europe/Amsterdam'
                       ) AS weekday
                FROM events e
                JOIN addresses a ON a.id = e.address_id
                CROSS JOIN LATERAL (
                    SELECT COUNT(*) AS going_count
                    FROM event_attendees ea
                    WHERE ea.event_id = e.id
                ) attendee_stats
                CROSS JOIN LATERAL (
                    SELECT COUNT(*) AS saved_count
                    FROM saved_events se
                    WHERE se.event_id = e.id
                ) saved_stats
                WHERE e.id <> :eventId
                  AND e.is_published = TRUE
                  AND e.is_cancelled = FALSE
                  AND e.end_at > now()
            )
            """;

    private static final String SIGNAL_SCORES_CTE = """
            signal_scores AS (
                SELECT ec.*,
                       shared_stats.shared_category_count,
                       unique_stats.unique_category_count,
                       COALESCE(
                           shared_stats.shared_category_count::DOUBLE PRECISION
                           / NULLIF(
                               unique_stats.unique_category_count,
                               0
                           ),
                           0
                       ) * 55 AS category_score, 
                       CASE
                               WHEN lower(trim(ec.city_name))
                                    = lower(trim(se.city_name))
                                   THEN 20
                               ELSE 0
                           END AS city_score,
                           CASE
                               WHEN ec.time_bucket = se.time_bucket
                                   THEN 15
                               ELSE 0
                           END AS time_score ,
                           CASE
                               WHEN ec.weekday = se.weekday
                                   THEN 7
                               ELSE 0
                           END AS weekday_score,
                           CASE
                               WHEN ec.price IS NULL
                                    AND se.price IS NULL
                                   THEN 3
                               WHEN ec.price = 0
                                    AND se.price = 0
                                   THEN 3
                               WHEN ec.price > 0
                                    AND se.price > 0
                                   THEN 3
                               ELSE 0
                           END AS price_score
                FROM eligible_candidates ec
                CROSS JOIN source_event se
                CROSS JOIN LATERAL (
                    SELECT COUNT(*) AS shared_category_count
                    FROM unnest(
                        ec.candidate_category_ids
                    ) AS candidate_category(category_id)
                    WHERE candidate_category.category_id
                          = ANY(se.category_ids)
                ) shared_stats
                CROSS JOIN LATERAL (
                    SELECT COUNT(DISTINCT category_id)
                           AS unique_category_count
                    FROM unnest(
                        se.category_ids
                        || ec.candidate_category_ids
                    ) AS combined_categories(category_id)
                ) unique_stats
            )
            """;

    private static final String TOTAL_SCORE_CTE = """
            scored_candidates AS (
                SELECT ss.*,
                       (
                           ss.category_score
                           + ss.city_score
                           + ss.time_score
                           + ss.weekday_score
                           + ss.price_score
            
                       ) AS similarity_score
                FROM signal_scores ss
            )
            """;
    private static final String RANKED_CANDIDATES_CTE = """
            ranked_candidates AS (
                SELECT *
                FROM scored_candidates
                ORDER BY similarity_score DESC,
                         popularity_score DESC,
                         start_at ASC,
                         candidate_id ASC
                LIMIT :limit
            )
            """;

    private static final String FIND_SIMILAR_EVENTS_SQL = """
            WITH
            %s,
            %s,
            %s,
            %s,
            %s
            SELECT e.id,
                   e.title,
                   ARRAY(
                       SELECT c.id
                       FROM event_categories ec
                       JOIN categories c ON c.id = ec.category_id
                       WHERE ec.event_id = e.id
                       ORDER BY c.name
                   ) AS category_ids,
                   ARRAY(
                       SELECT c.name
                       FROM event_categories ec
                       JOIN categories c ON c.id = ec.category_id
                       WHERE ec.event_id = e.id
                       ORDER BY c.name
                   ) AS category_names,
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
                          (
                              SELECT ei.image_url
                              FROM event_images ei
                              WHERE ei.event_id = e.id
                              ORDER BY ei.created_at, ei.id
                              LIMIT 1
                          ) AS image_url,
                          rc.going_count,
                          e.is_cancelled,
                          rc.similarity_score
                   FROM ranked_candidates rc
                   JOIN events e ON e.id = rc.candidate_id
                   JOIN addresses a ON a.id = e.address_id
                   ORDER BY rc.similarity_score DESC,
                            rc.popularity_score DESC,
                            e.start_at ASC,
                            e.id ASC
            """.formatted(
            SOURCE_EVENT_CTE,
            ELIGIBLE_CANDIDATES_CTE,
            SIGNAL_SCORES_CTE,
            TOTAL_SCORE_CTE,
            RANKED_CANDIDATES_CTE
    );


    public List<SimilarEventCandidate> findSimilarEvents(
            UUID eventId,
            int limit
    ) {
        return jdbcClient
                .sql(FIND_SIMILAR_EVENTS_SQL)
                .param("eventId", eventId)
                .param("limit", limit)
                .query(SIMILAR_EVENT_ROW_MAPPER)
                .list();

    }
}
