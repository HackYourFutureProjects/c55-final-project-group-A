package nl.hackyourfuture.project.backend.notification.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EventReminderRepository {

    public record EventReminderCandidate(
            UUID userId,
            UUID eventId,
            String eventTitle
    ) {}

    private final JdbcClient jdbcClient;

    public List<EventReminderCandidate> findGoingUsersWithEventsStartingBetween(
            OffsetDateTime windowStart,
            OffsetDateTime windowEnd
    ) {
        return jdbcClient
                .sql("""
                        SELECT ea.user_id, ef.id AS event_id, ef.title AS event_title
                        FROM event_attendees ea
                        JOIN event_feed ef ON ef.id = ea.event_id
                        WHERE ef.is_published = TRUE
                          AND ef.is_cancelled = FALSE
                          AND ef.start_at >= :windowStart
                          AND ef.start_at < :windowEnd
                        """)
                .param("windowStart", windowStart)
                .param("windowEnd", windowEnd)
                .query((rs, _) -> new EventReminderCandidate(
                        rs.getObject("user_id", UUID.class),
                        rs.getObject("event_id", UUID.class),
                        rs.getString("event_title")
                ))
                .list();
    }
}
