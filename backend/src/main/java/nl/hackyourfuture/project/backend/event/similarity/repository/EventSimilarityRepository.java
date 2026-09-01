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

    private static final int CATEGORY_WEIGHT = 55;
    private static final int CITY_WEIGHT = 20;
    private static final int TIME_WEIGHT = 15;
    private static final int WEEKDAY_WEIGHT = 7;
    private static final int PRICE_WEIGHT = 3;

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
                    FROM event_feed
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
                       e.city_name,
                       e.start_at,
                       e.category_ids,
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
                       ) AS weekday,
                       CASE
                           WHEN e.price IS NULL THEN 'UNKNOWN'
                           WHEN e.price = 0 THEN 'FREE'
                           ELSE 'PAID'
                       END AS price_bucket
                FROM event_feed e
                WHERE e.id = :eventId
                  AND e.is_published = TRUE
            )
            """;

    private static final String ELIGIBLE_CANDIDATES_CTE = """
            eligible_candidates AS (
                SELECT e.id AS candidate_id,
                       e.city_name,
                       e.start_at,
                       e.category_ids AS candidate_category_ids,
                       e.going_count,
                       e.popularity_score,
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
                       ) AS weekday,
                       CASE
                           WHEN e.price IS NULL THEN 'UNKNOWN'
                           WHEN e.price = 0 THEN 'FREE'
                           ELSE 'PAID'
                       END AS price_bucket
                FROM event_feed e
                WHERE e.id <> :eventId
                  AND e.is_published = TRUE
                  AND e.is_cancelled = FALSE
                  AND (
                      e.end_at > now()
                      OR (
                          e.end_at IS NULL
                          AND (e.start_at AT TIME ZONE 'Europe/Amsterdam')::date
                              >= (now() AT TIME ZONE 'Europe/Amsterdam')::date
                      )
                  )
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
                       ) * :categoryWeight AS category_score,
                       CASE
                           WHEN lower(trim(ec.city_name))
                                = lower(trim(se.city_name))
                               THEN :cityWeight
                           ELSE 0
                       END AS city_score,
                       CASE
                           WHEN ec.time_bucket = se.time_bucket
                               THEN :timeWeight
                           ELSE 0
                       END AS time_score,
                       CASE
                           WHEN ec.weekday = se.weekday
                               THEN :weekdayWeight
                           ELSE 0
                       END AS weekday_score,
                       CASE
                           WHEN ec.price_bucket = se.price_bucket
                               THEN :priceWeight
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
                   e.category_ids,
                   e.category_names,
                   e.start_at,
                   e.end_at,
                   e.price,
                   e.street,
                   e.house_number,
                   e.postal_code,
                   e.city_name,
                   e.province,
                   e.latitude,
                   e.longitude,
                   e.image_url,
                   rc.going_count,
                   e.is_cancelled,
                   rc.similarity_score
            FROM ranked_candidates rc
            JOIN event_feed e ON e.id = rc.candidate_id
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
                .param("categoryWeight", CATEGORY_WEIGHT)
                .param("cityWeight", CITY_WEIGHT)
                .param("timeWeight", TIME_WEIGHT)
                .param("weekdayWeight", WEEKDAY_WEIGHT)
                .param("priceWeight", PRICE_WEIGHT)
                .query(SIMILAR_EVENT_ROW_MAPPER)
                .list();
    }
}
