package nl.hackyourfuture.project.backend.notification.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FeedbackNotificationRepository {

    public record FeedbackNotificationCandidate(
            UUID feedbackId,
            String eventTitle
    ) {
    }

    private final JdbcClient jdbcClient;

    public List<FeedbackNotificationCandidate> findUnnotifiedFeedbackCreatedAfter(
            OffsetDateTime createdAfter,
            int limit
    ) {
        return jdbcClient
                .sql("""
                        SELECT f.id, f.event_title
                        FROM feedbacks f
                        WHERE f.created_at >= :createdAfter
                          AND NOT EXISTS (
                              SELECT 1
                              FROM notifications n
                              WHERE n.type = 'NEW_FEEDBACK'
                                AND n.resource_id = f.id
                          )
                        ORDER BY f.created_at ASC, f.id ASC
                        LIMIT :limit
                        """)
                .param("createdAfter", createdAfter)
                .param("limit", limit)
                .query((rs, _) -> new FeedbackNotificationCandidate(
                        rs.getObject("id", UUID.class),
                        rs.getString("event_title")
                ))
                .list();
    }
}
