package nl.hackyourfuture.project.backend.event.repository;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.category.model.Category;
import nl.hackyourfuture.project.backend.event.model.AdminEventDetail;
import nl.hackyourfuture.project.backend.event.model.AdminEventSummary;
import nl.hackyourfuture.project.backend.event.model.EventUpdate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AdminEventRepository {

    private final JdbcClient jdbcClient;

    private static final RowMapper<AdminEventSummary>
            ADMIN_EVENT_SUMMARY_ROW_MAPPER =
            (rs, _) -> new AdminEventSummary(
                    rs.getObject("id", UUID.class),
                    rs.getString("title"),
                    rs.getObject("start_at", OffsetDateTime.class),
                    rs.getObject("end_at", OffsetDateTime.class),
                    rs.getString("city_name"),
                    rs.getString("image_url"),
                    rs.getBoolean("is_published"),
                    rs.getBoolean("is_cancelled")
            );

    public List<AdminEventSummary> findAdminEventSummaries(
            int limit,
            int offset
    ) {
        String sql = """
                SELECT e.id,
                       e.title,
                       e.start_at,
                       e.end_at,
                       a.city_name,
                       (
                           SELECT ei.image_url
                           FROM event_images ei
                           WHERE ei.event_id = e.id
                           ORDER BY ei.created_at, ei.id
                           LIMIT 1
                       ) AS image_url,
                       e.is_published,
                       e.is_cancelled
                FROM events e
                JOIN addresses a ON a.id = e.address_id
                ORDER BY e.created_at DESC, e.id
                LIMIT :limit
                OFFSET :offset
                """;

        return jdbcClient
                .sql(sql)
                .param("limit", limit)
                .param("offset", offset)
                .query(ADMIN_EVENT_SUMMARY_ROW_MAPPER)
                .list();
    }

    public long countAdminEvents() {
        return jdbcClient
                .sql("SELECT COUNT(*) FROM events")
                .query(Long.class)
                .single();
    }

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

    private static final RowMapper<AdminEventDetail> ADMIN_DETAIL_ROW_MAPPER =
            (rs, _) -> new AdminEventDetail(
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
                    rs.getBoolean("is_published"),
                    rs.getBoolean("is_cancelled")
            );

    public Optional<AdminEventDetail> findEventDetailById(UUID eventId) {
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
                       e.is_published,
                       e.is_cancelled
                FROM events e
                JOIN addresses a ON a.id = e.address_id
                WHERE e.id = :eventId
                """;

        return jdbcClient
                .sql(sql)
                .param("eventId", eventId)
                .query(ADMIN_DETAIL_ROW_MAPPER)
                .optional();

    }

    public boolean updateEvent(EventUpdate event) {
        String sql = """
                UPDATE events
                SET title = :title,
                    description = :description,
                    start_at = :startAt,
                    end_at = :endAt,
                    price = :price,
                    updated_at = now()
                WHERE id = :eventId
                """;

        return jdbcClient
                .sql(sql)
                .param("title", event.title())
                .param("description", event.description())
                .param("startAt", event.startAt())
                .param("endAt", event.endAt())
                .param("price", event.price())
                .param("eventId", event.id())
                .update() == 1;
    }

    public boolean cancelEvent(UUID eventId) {
        return jdbcClient
                .sql("""
                        UPDATE events
                        SET is_cancelled = TRUE,
                            updated_at = now()
                        WHERE id = :eventId
                        """)
                .param("eventId", eventId)
                .update() == 1;
    }

    public Optional<UUID> deleteEventById(UUID eventId) {
        return jdbcClient
                .sql("""
                        DELETE FROM events
                        WHERE id = :eventId
                        RETURNING address_id
                        """)
                .param("eventId", eventId)
                .query(UUID.class)
                .optional();
    }

}
