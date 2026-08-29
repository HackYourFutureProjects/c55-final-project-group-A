package nl.hackyourfuture.project.backend.feedback;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FeedbackRepository {
  public static final RowMapper<Feedback> FEEDBACK_ROW_MAPPER = (rs, _) -> Feedback.builder()
      .id(rs.getObject("id", UUID.class))
      .topic(Topic.fromDbValue(rs.getString("topic")))
      .eventTitle(rs.getString("event_title"))
      .rating(rs.getInt("rating"))
      .message(rs.getString("message"))
      .senderName(rs.getString("sender_name"))
      .senderEmail(rs.getString("sender_email"))
      .isReviewed(rs.getBoolean("is_reviewed"))
      .createdAt(rs.getObject("created_at", OffsetDateTime.class))
      .build();
  private final JdbcClient jdbcClient;

  public void createFeedback(Feedback feedback) {
    jdbcClient
        .sql("""
            INSERT INTO feedbacks (topic, event_title, rating, message, sender_name, sender_email)
            VALUES(:topic, :eventTitle, :rating, :message, :senderName, :senderEmail)
            """)
        .param("topic", feedback.getTopic().toDbValue())
        .param("eventTitle", feedback.getEventTitle())
        .param("rating", feedback.getRating())
        .param("message", feedback.getMessage())
        .param("senderName", feedback.getSenderName())
        .param("senderEmail", feedback.getSenderEmail())
        .update();
  }

  public List<Feedback> getAllFeedbacks(int limit, int offset) {
    return jdbcClient
        .sql("""
            SELECT id, topic, event_title, rating, message, sender_name, sender_email, is_reviewed, created_at
            FROM feedbacks
            ORDER BY created_at DESC
            LIMIT :limit
            OFFSET :offset
            """)
        .param("limit", limit)
        .param("offset", offset)
        .query(FEEDBACK_ROW_MAPPER)
        .list();

  }

  public Optional<Feedback> findFeedbackById(UUID id) {
    return jdbcClient
        .sql("""
            SELECT id, topic, event_title, rating, message, sender_name, sender_email, is_reviewed, created_at
            FROM feedbacks WHERE id = :id
            """)
        .param("id", id)
        .query(FEEDBACK_ROW_MAPPER)
        .optional();
  }

  public Feedback updateReviewedStatusOfFeedback(Feedback feedback) {
    return jdbcClient.sql("""
            UPDATE feedbacks
            SET is_reviewed = :isReviewed
            WHERE id = :id
            RETURNING id, topic, event_title, rating, message, sender_name, sender_email, is_reviewed, created_at
            """)
        .param("id", feedback.getId())
        .param("isReviewed", feedback.isReviewed())
        .query(FEEDBACK_ROW_MAPPER)
        .single();

  }

  public long countFeedbacks() {
    return jdbcClient
        .sql("""
            SELECT COUNT(*) FROM feedbacks
            """)
        .query(Long.class)
        .single();
  }
}