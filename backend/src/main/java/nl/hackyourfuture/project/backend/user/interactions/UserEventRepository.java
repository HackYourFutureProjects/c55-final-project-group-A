package nl.hackyourfuture.project.backend.user.interactions;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserEventRepository {

  public static final RowMapper<SavedGoingEventCard> SAVED_GOING_EVENT_CARD_ROW_MAPPER = (rs, _) -> {
    UUID[] categoryIds = (UUID[]) rs.getArray("category_ids").getArray();
    String[] categoryNames = (String[]) rs.getArray("category_names").getArray();

    List<SavedGoingEventCard.CategoryName> categories = new ArrayList<>();

    for (int i = 0; i < categoryIds.length; i++) {
      categories.add(new SavedGoingEventCard.CategoryName(categoryIds[i], categoryNames[i]));

    }

    return new SavedGoingEventCard(
        rs.getObject("id", UUID.class),
        rs.getString("title"),
        rs.getString("image_url"),
        categories,
        rs.getObject("start_at", OffsetDateTime.class),
        rs.getObject("end_at", OffsetDateTime.class),
        rs.getBigDecimal("price"),
        rs.getString("street"),
        rs.getString("house_number"),
        rs.getString("city_name"),
        rs.getString("province"),
        rs.getBoolean("is_cancelled")
    );

  };
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

  public boolean eventExists(UUID eventId) {
    return jdbcClient
        .sql("SELECT EXISTS(SELECT 1 FROM event_feed WHERE id = :eventId AND is_published = TRUE)")
        .param("eventId", eventId)
        .query(Boolean.class)
        .single();
  }

  public List<SavedGoingEventCard> getSavedEvents(UUID userId, int limit, int offset) {
    return jdbcClient
        .sql("""
            SELECT e.id, e.title, e.image_url,
            e.category_ids, e,category_names,
            e.start_at, e.end_at, e.price,
            e.street, e.house_number, e.city_name, e.province,
            e.is_cancelled
            FROM saved_events se
            JOIN event_feed e ON e.id = se.event_id
            WHERE se.user_id = :userId
            ORDER BY e.start_at
            LIMIT :limit
            OFFSET :offset
            """
        )
        .param("userId", userId)
        .param("limit", limit)
        .param("offset", offset)
        .query(SAVED_GOING_EVENT_CARD_ROW_MAPPER)
        .list();
  }

  public long countSavedByUser(UUID userId) {
    return jdbcClient
        .sql("""
            SELECT COUNT(*) FROM saved_events WHERE user_id = :userId
            """)
        .param("userId", userId)
        .query(Long.class)
        .single();
  }

  public List<SavedGoingEventCard> getGoingEvents(UUID userId, int limit, int offset) {
    return jdbcClient
        .sql("""
            SELECT e.id, e.title, e.image_url,
            e.category_ids, e.category_names,
            e.start_at, e.end_at, e.price,
            e.street, e.house_number, e.city_name, e.province, 
            e.is_cancelled
            FROM event_attendees ea
            JOIN event_feed e ON e.id = ea.event_id
            WHERE ea.user_id = :userId
            ORDER BY e.start_at
            LIMIT :limit
            OFFSET :offset
            """)
        .param("userId", userId)
        .param("limit", limit)
        .param("offset", offset)
        .query(SAVED_GOING_EVENT_CARD_ROW_MAPPER)
        .list();
  }

  public long countGoingByUser(UUID userId) {
    return jdbcClient
        .sql("""
            SELECT COUNT(*) FROM event_attendees WHERE user_id = :userId
            """)
        .param("userId", userId)
        .query(Long.class)
        .single();
  }


}

