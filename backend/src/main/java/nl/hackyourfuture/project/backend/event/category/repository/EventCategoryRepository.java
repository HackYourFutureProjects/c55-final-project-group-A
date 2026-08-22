package nl.hackyourfuture.project.backend.event.category.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EventCategoryRepository {

    private final JdbcClient jdbcClient;

    public void addCategories(
            UUID eventId,
            Set<UUID> categoryIds
    ) {
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

    public void replaceCategories(
            UUID eventId,
            Set<UUID> categoryIds
    ) {
        removeCategories(eventId);
        addCategories(eventId, categoryIds);
    }

    public void removeCategories(UUID eventId) {
        jdbcClient
                .sql("""
                        DELETE FROM event_categories
                        WHERE event_id = :eventId
                        """)
                .param("eventId", eventId)
                .update();
    }
}
