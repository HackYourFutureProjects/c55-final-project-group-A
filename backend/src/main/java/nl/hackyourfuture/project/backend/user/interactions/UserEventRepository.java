package nl.hackyourfuture.project.backend.user.interactions;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserEventRepository {

  private final JdbcClient jdbcClient;

  public void addEventToSaved(UUID userId, UUID eventId) {
    jdbcClient.sql("""
            INSERT INTO saved_events (user_id, event_id)
            VALUES (:userId, :eventId)
            """)
        .param("userId", userId)
        .param("eventId", eventId)
        .update();

  }

  public void addEventToGoing(UUID userId, UUID eventId) {
    jdbcClient.sql("""
            INSERT INTO event_attendees (user_id, event_id)
            VALUES (:userId, :eventId)
            """)
        .param("userId", userId)
        .param("eventId", eventId)
        .update();
  }

  public void deleteEventFromSaved(UUID userId, UUID eventId) {
    jdbcClient.sql("""
            DELETE FROM saved_events
            WHERE user_id = :userId AND event_id = :eventId
            """)
        .param("userId", userId)
        .param("eventId", eventId)
        .update();

  }

  public void deleteEventFromGoing(UUID userId, UUID eventId) {
    jdbcClient.sql("""
            DELETE FROM event_attendees
            WHERE user_id = :userId AND event_id = :eventId
            """)
        .param("userId", userId)
        .param("eventId", eventId)
        .update();
  }


}

