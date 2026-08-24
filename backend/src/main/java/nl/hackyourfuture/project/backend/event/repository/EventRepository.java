package nl.hackyourfuture.project.backend.event.repository;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.category.model.Category;
import nl.hackyourfuture.project.backend.event.model.EventDetail;
import nl.hackyourfuture.project.backend.event.model.EventQueryCriteria;
import nl.hackyourfuture.project.backend.event.model.EventSort;
import nl.hackyourfuture.project.backend.event.model.EventSummary;
import nl.hackyourfuture.project.backend.event.model.NewEvent;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EventRepository {

    private final JdbcClient jdbcClient;

    private static final String CATEGORY_FILTER_CLAUSE = """
              AND EXISTS (
                  SELECT 1
                  FROM event_categories filter_ec
                  WHERE filter_ec.event_id = e.id
                    AND filter_ec.category_id IN (:categoryIds)
              )
            """;

    private static final String DATE_FILTER_CLAUSE = """
              AND e.start_at < :dateToExclusive
              AND e.end_at > :dateFromStart
            """;

    private static final ZoneId EVENT_TIME_ZONE =
            ZoneId.of("Europe/Amsterdam");

    private static final String LOCATION_FILTER_CLAUSE = """
              AND (
                  6371.0088 * 2 * ASIN(
                      SQRT(
                          LEAST(
                              1.0,
                              POWER(
                                  SIN(
                                      RADIANS(
                                          CAST(a.latitude AS DOUBLE PRECISION)
                                          - CAST(:latitude AS DOUBLE PRECISION)
                                      ) / 2
                                  ),
                                  2
                              )
                              + COS(
                                  RADIANS(
                                      CAST(:latitude AS DOUBLE PRECISION)
                                  )
                              )
                              * COS(
                                  RADIANS(
                                      CAST(a.latitude AS DOUBLE PRECISION)
                                  )
                              )
                              * POWER(
                                  SIN(
                                      RADIANS(
                                          CAST(a.longitude AS DOUBLE PRECISION)
                                          - CAST(:longitude AS DOUBLE PRECISION)
                                      ) / 2
                                  ),
                                  2
                              )
                          )
                      )
                  )
              ) <= CAST(:radiusKm AS DOUBLE PRECISION)
            """;
    private static final String PRICE_FILTER_CLAUSE = """
              AND (
                  (:price = 'FREE' AND e.price = 0)
                  OR (:price = 'PAID' AND e.price > 0)
              )
            """;

    private static final String TIME_OF_DAY_FILTER_CLAUSE = """
              AND CASE
                  WHEN CAST(
                      e.start_at AT TIME ZONE 'Europe/Amsterdam' AS TIME
                  ) >= TIME '06:00:00'
                  AND CAST(
                      e.start_at AT TIME ZONE 'Europe/Amsterdam' AS TIME
                  ) < TIME '12:00:00'
                      THEN 'MORNING'
                  WHEN CAST(
                      e.start_at AT TIME ZONE 'Europe/Amsterdam' AS TIME
                  ) >= TIME '12:00:00'
                  AND CAST(
                      e.start_at AT TIME ZONE 'Europe/Amsterdam' AS TIME
                  ) < TIME '18:00:00'
                      THEN 'AFTERNOON'
                  ELSE 'EVENING'
              END IN (:timesOfDay)
            """;

    private static String orderByClause(EventSort sort) {
        return switch (sort) {
            case START_TIME_ASC -> " ORDER BY e.start_at ASC, e.id ASC ";
            case POPULARITY_DESC -> " ORDER BY popularity_score DESC, e.start_at ASC, e.id ASC ";
            case PRICE_ASC -> " ORDER BY e.price ASC, e.start_at ASC, e.id ASC ";
            case PRICE_DESC -> " ORDER BY e.price DESC, e.start_at ASC, e.id ASC ";
        };
    }

    private static final RowMapper<EventSummary> EVENT_SUMMARY_ROW_MAPPER =
            (rs, _) -> new EventSummary(
                    rs.getObject("id", UUID.class),
                    rs.getString("title"),
                    mapCategories(rs),
                    rs.getObject("start_at", OffsetDateTime.class),
                    rs.getObject("end_at", OffsetDateTime.class),
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

    private static List<Category> mapCategories(
            java.sql.ResultSet resultSet
    ) throws SQLException {
        UUID[] categoryIds =
                (UUID[]) resultSet.getArray("category_ids").getArray();

        String[] categoryNames =
                (String[]) resultSet.getArray("category_names").getArray();

        List<Category> categories = new ArrayList<>(categoryIds.length);

        for (int index = 0; index < categoryIds.length; index++) {
            categories.add(new Category(
                    categoryIds[index],
                    categoryNames[index]
            ));
        }

        return List.copyOf(categories);
    }

    public List<EventSummary> findEventSummaries(
            EventQueryCriteria criteria,
            int limit,
            int offset
    ) {
        boolean filterByCategory = criteria.hasCategoryFilter();
        boolean filterByDate = criteria.hasCompleteDateFilter();
        boolean filterByLocation = criteria.hasCompleteLocationFilter();
        boolean filterByPrice = criteria.hasPriceFilter();
        boolean filterByTimeOfDay = criteria.hasTimeOfDayFilter();
        String orderBy = orderByClause(criteria.sort());

        String sql = """
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
                       attendee_stats.going_count AS going_count,
                       (
                           3 * attendee_stats.going_count
                           + saved_stats.saved_count
                       ) AS popularity_score,
                       e.is_cancelled
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
                WHERE e.is_published = TRUE
                  AND e.is_cancelled = FALSE
                  AND e.end_at > now()
                  AND (
                      e.title ILIKE '%' || COALESCE(:search, '') || '%'
                      OR COALESCE(e.description, '') ILIKE '%' || COALESCE(:search, '') || '%'
                      OR a.city_name ILIKE '%' || COALESCE(:search, '') || '%'
                      OR EXISTS (
                          SELECT 1
                          FROM event_categories ec
                          JOIN categories c ON c.id = ec.category_id
                          WHERE ec.event_id = e.id
                            AND c.name ILIKE '%' || COALESCE(:search, '') || '%'
                      )
                  )
                """ + (filterByCategory ? CATEGORY_FILTER_CLAUSE : "")
                + (filterByDate ? DATE_FILTER_CLAUSE : "")
                + (filterByLocation ? LOCATION_FILTER_CLAUSE : "")
                + (filterByPrice ? PRICE_FILTER_CLAUSE : "")
                + (filterByTimeOfDay ? TIME_OF_DAY_FILTER_CLAUSE : "")
                + orderBy + """
                LIMIT :limit
                OFFSET :offset
                """;

        var statement = jdbcClient
                .sql(sql)
                .param("search", criteria.search())
                .param("limit", limit)
                .param("offset", offset);

        if (filterByCategory) {
            statement = statement.param(
                    "categoryIds",
                    criteria.categoryIds()
            );
        }

        if (filterByDate) {
            statement = statement
                    .param(
                            "dateFromStart",
                            criteria.dateFrom()
                                    .atStartOfDay(EVENT_TIME_ZONE)
                                    .toOffsetDateTime()
                    )
                    .param(
                            "dateToExclusive",
                            criteria.dateTo()
                                    .plusDays(1)
                                    .atStartOfDay(EVENT_TIME_ZONE)
                                    .toOffsetDateTime()
                    );
        }
        if (filterByLocation) {
            statement = statement
                    .param("latitude", criteria.latitude())
                    .param("longitude", criteria.longitude())
                    .param("radiusKm", criteria.radiusKm());
        }
        if (filterByPrice) {
            statement = statement
                    .param("price", criteria.price().name());
        }

        if (filterByTimeOfDay) {
            statement = statement.param(
                    "timesOfDay",
                    criteria.timesOfDay().stream()
                            .map(Enum::name)
                            .toList()
            );
        }

        return statement
                .query(EVENT_SUMMARY_ROW_MAPPER)
                .list();
    }

    public long countEvents(EventQueryCriteria criteria) {
        boolean filterByCategory = criteria.hasCategoryFilter();
        boolean filterByDate = criteria.hasCompleteDateFilter();
        boolean filterByLocation = criteria.hasCompleteLocationFilter();
        boolean filterByPrice = criteria.hasPriceFilter();
        boolean filterByTimeOfDay = criteria.hasTimeOfDayFilter();

        String sql = """
                SELECT COUNT(*)
                FROM events e
                JOIN addresses a ON a.id = e.address_id
                WHERE e.is_published = TRUE
                  AND e.is_cancelled = FALSE
                  AND e.end_at > now()
                  AND (
                      e.title ILIKE '%' || COALESCE(:search, '') || '%'
                      OR COALESCE(e.description, '') ILIKE '%' || COALESCE(:search, '') || '%'
                      OR a.city_name ILIKE '%' || COALESCE(:search, '') || '%'
                      OR EXISTS (
                          SELECT 1
                          FROM event_categories ec
                          JOIN categories c ON c.id = ec.category_id
                          WHERE ec.event_id = e.id
                            AND c.name ILIKE '%' || COALESCE(:search, '') || '%'
                      )
                  )
                """ + (filterByCategory ? CATEGORY_FILTER_CLAUSE : "")
                + (filterByDate ? DATE_FILTER_CLAUSE : "")
                + (filterByLocation ? LOCATION_FILTER_CLAUSE : "")
                + (filterByPrice ? PRICE_FILTER_CLAUSE : "")
                + (filterByTimeOfDay ? TIME_OF_DAY_FILTER_CLAUSE : "");

        var statement = jdbcClient
                .sql(sql)
                .param("search", criteria.search());

        if (filterByCategory) {
            statement = statement.param(
                    "categoryIds",
                    criteria.categoryIds()
            );
        }

        if (filterByDate) {
            statement = statement
                    .param(
                            "dateFromStart",
                            criteria.dateFrom()
                                    .atStartOfDay(EVENT_TIME_ZONE)
                                    .toOffsetDateTime()
                    )
                    .param(
                            "dateToExclusive",
                            criteria.dateTo()
                                    .plusDays(1)
                                    .atStartOfDay(EVENT_TIME_ZONE)
                                    .toOffsetDateTime()
                    );
        }

        if (filterByLocation) {
            statement = statement
                    .param("latitude", criteria.latitude())
                    .param("longitude", criteria.longitude())
                    .param("radiusKm", criteria.radiusKm());
        }
        if (filterByPrice) {
            statement = statement
                    .param("price", criteria.price().name());
        }

        if (filterByTimeOfDay) {
            statement = statement.param(
                    "timesOfDay",
                    criteria.timesOfDay().stream()
                            .map(Enum::name)
                            .toList()
            );
        }

        return statement
                .query(Long.class)
                .single();
    }

    private static final RowMapper<EventDetail> EVENT_DETAIL_ROW_MAPPER =
            (rs, _) -> new EventDetail(
                    rs.getObject("id", UUID.class),
                    rs.getString("title"),
                    rs.getString("description"),
                    mapCategories(rs),
                    rs.getObject("start_at", OffsetDateTime.class),
                    rs.getObject("end_at", OffsetDateTime.class),
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

    public Optional<EventDetail> findEventDetailById(UUID eventId) {
        String sql = """
                SELECT e.id,
                       e.title,
                       e.description,
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
                       (
                           SELECT COUNT(*)
                           FROM event_attendees ea
                           WHERE ea.event_id = e.id
                       ) AS going_count,
                       e.is_cancelled
                FROM events e
                JOIN addresses a ON a.id = e.address_id
                WHERE e.id = :eventId
                  AND e.is_published = TRUE
                """;

        return jdbcClient
                .sql(sql)
                .param("eventId", eventId)
                .query(EVENT_DETAIL_ROW_MAPPER)
                .optional();
    }

    public UUID createEvent(NewEvent event) {
        String sql = """
                INSERT INTO events (
                    title,
                    description,
                    address_id,
                    start_at,
                    end_at,
                    price,
                    created_by_user_id
                )
                VALUES (
                    :title,
                    :description,
                    :addressId,
                    :startAt,
                    :endAt,
                    :price,
                    :createdByUserId
                )
                RETURNING id
                """;

        return jdbcClient
                .sql(sql)
                .param("title", event.title())
                .param("description", event.description())
                .param("addressId", event.addressId())
                .param("startAt", event.startAt())
                .param("endAt", event.endAt())
                .param("price", event.price())
                .param("createdByUserId", event.createdByUserId())
                .query(UUID.class)
                .single();
    }

    public boolean existsById(UUID eventId) {
        return jdbcClient
                .sql("""
                        SELECT EXISTS(
                            SELECT 1
                            FROM events
                            WHERE id = :eventId
                        )
                        """)
                .param("eventId", eventId)
                .query(Boolean.class)
                .single();
    }

    public boolean isCancelled(UUID eventId) {
        return jdbcClient
                .sql("""
                        SELECT is_cancelled
                        FROM events
                        WHERE id = :eventId
                        """)
                .param("eventId", eventId)
                .query(Boolean.class)
                .single();
    }

    public void publish(UUID eventId) {
        jdbcClient
                .sql("""
                        UPDATE events
                        SET is_published = TRUE
                        WHERE id = :eventId
                        """)
                .param("eventId", eventId)
                .update();
    }
}
