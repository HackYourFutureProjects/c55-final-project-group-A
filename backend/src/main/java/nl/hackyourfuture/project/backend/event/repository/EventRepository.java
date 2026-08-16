package nl.hackyourfuture.project.backend.event.repository;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.model.EventSummary;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EventRepository {

    private final JdbcClient jdbcClient;

    private static final RowMapper<EventSummary> EVENT_SUMMARY_ROW_MAPPER =
            (rs, _) -> new EventSummary(
                    rs.getObject("id", UUID.class),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("category_name"),
                    rs.getObject("start_at", OffsetDateTime.class),
                    rs.getObject("end_at", OffsetDateTime.class),
                    rs.getBigDecimal("price"),
                    rs.getString("street"),
                    rs.getString("house_number"),
                    rs.getString("postal_code"),
                    rs.getString("city_name"),
                    rs.getString("province"),
                    rs.getString("image_url"),
                    rs.getLong("going_count"),
                    rs.getBoolean("is_cancelled")
            );

    public List<EventSummary> findEventSummaries(String search, int limit,
                                                 int offset) {
        String sql = """
                SELECT e.id,
                       e.title,
                       e.description,
                       c.name AS category_name,
                       e.start_at,
                       e.end_at,
                       e.price,
                       a.street,
                       a.house_number,
                       a.postal_code,
                       ci.name AS city_name,
                       ci.province,
                       (
                           SELECT ei.image_key
                           FROM event_images ei
                           WHERE ei.event_id = e.id
                           ORDER BY ei.created_at
                           LIMIT 1
                       ) AS image_url,
                       (
                           SELECT COUNT(*)
                           FROM event_attendees ea
                           WHERE ea.event_id = e.id
                       ) AS going_count,
                       e.is_cancelled
                FROM events e
                JOIN categories c ON c.id = e.category_id
                JOIN addresses a ON a.id = e.address_id
                JOIN cities ci ON ci.id = a.city_id
                WHERE e.title ILIKE '%' || COALESCE(:search, '') || '%'
                ORDER BY e.start_at, e.id
                LIMIT :limit
                OFFSET :offset
                """;

        return jdbcClient
                .sql(sql)
                .param("search", search)
                .param("limit", limit)
                .param("offset", offset)
                .query(EVENT_SUMMARY_ROW_MAPPER)
                .list();
    }

    public long countEvents(String search) {
        String sql = """
                SELECT COUNT(*)
                FROM events e
                WHERE e.title ILIKE '%' || COALESCE(:search, '') || '%'
                """;

        return jdbcClient
                .sql(sql)
                .param("search", search)
                .query(Long.class)
                .single();
    }
}