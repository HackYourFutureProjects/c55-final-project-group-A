package nl.hackyourfuture.project.backend.event.image.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EventImageRepository {

    private final JdbcClient jdbcClient;

    public void save(
            UUID eventId,
            String imageUrl,
            String contentType
    ) {
        jdbcClient
                .sql("""
                        DELETE FROM event_images
                        WHERE event_id = :eventId
                        """)
                .param("eventId", eventId)
                .update();

        jdbcClient
                .sql("""
                        INSERT INTO event_images (
                            event_id,
                            image_url,
                            content_type
                        )
                        VALUES (
                            :eventId,
                            :imageUrl,
                            :contentType
                        )
                        """)
                .param("eventId", eventId)
                .param("imageUrl", imageUrl)
                .param("contentType", contentType)
                .update();
    }

    public boolean existsByEventId(UUID eventId) {
        return jdbcClient
                .sql("""
                        SELECT EXISTS(
                            SELECT 1
                            FROM event_images
                            WHERE event_id = :eventId
                        )
                        """)
                .param("eventId", eventId)
                .query(Boolean.class)
                .single();
    }
}
