package nl.hackyourfuture.project.backend.event.repository;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.model.EventDetail;
import nl.hackyourfuture.project.backend.event.model.EventDraft;
import nl.hackyourfuture.project.backend.event.model.EventSummary;
import nl.hackyourfuture.project.backend.event.category.model.Category;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;


@Repository
@RequiredArgsConstructor
public class EventRepository {

    private final JdbcClient jdbcClient;

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

    public List<EventSummary> findEventSummaries(String search, int limit, int offset) {
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
                       (
                           SELECT ei.image_key
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
                WHERE e.is_published = TRUE
                  AND e.title ILIKE '%' || COALESCE(:search, '') || '%'
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
                WHERE e.is_published = TRUE
                  AND e.title ILIKE '%' || COALESCE(:search, '') || '%'
                """;

        return jdbcClient
                .sql(sql)
                .param("search", search)
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
                       (
                           SELECT ei.image_key
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

    public UUID createDraft(EventDraft draft) {
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
                .param("title", draft.title())
                .param("description", draft.description())
                .param("addressId", draft.addressId())
                .param("startAt", draft.startAt())
                .param("endAt", draft.endAt())
                .param("price", draft.price())
                .param("createdByUserId", draft.createdByUserId())
                .query(UUID.class)
                .single();
    }

    public void addCategories(UUID eventId, Set<UUID> categoryIds) {
        for (UUID categoryId : categoryIds) {
            jdbcClient
                    .sql("""
                            INSERT INTO event_categories (event_id, category_id)
                            VALUES (:eventId, :categoryId)
                            """)
                    .param("eventId", eventId)
                    .param("categoryId", categoryId)
                    .update();
        }
    }
}
