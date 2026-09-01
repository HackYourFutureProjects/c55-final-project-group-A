package nl.hackyourfuture.project.backend.event.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EventRegistryRepository {

    private final JdbcClient jdbcClient;

    public void registerEventIfMissing(UUID eventId) {
        jdbcClient.sql("""
                        INSERT INTO event_registry (
                            id,
                            source,
                            internal_event_id,
                            external_event_key
                        )
                        SELECT
                            e.id,
                            e.source,
                            CASE WHEN e.source = 'app' THEN e.id END,
                            CASE WHEN e.source = 'app' THEN NULL ELSE e.stable_event_key END
                        FROM event_feed e
                        WHERE e.id = :eventId
                          AND e.is_published = TRUE
                        ON CONFLICT DO NOTHING
                        """)
                .param("eventId", eventId)
                .update();
    }
}
