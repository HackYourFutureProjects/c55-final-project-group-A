package nl.hackyourfuture.project.backend.notification.repository;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.feedback.Topic;
import nl.hackyourfuture.project.backend.notification.model.NotificationOutbox;
import nl.hackyourfuture.project.backend.notification.model.NotificationType;
import nl.hackyourfuture.project.backend.notification.model.OutboxPayload;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationOutboxRepository {

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    private static final String OUTBOX_COLUMNS = """
            id,
            type,
            resource_id,
            payload,
            created_at,
            processed_at
            """;

    private RowMapper<NotificationOutbox> outboxRowMapper() {
        return (rs, _) -> new NotificationOutbox(
                rs.getObject("id", UUID.class),
                NotificationType.fromDbValue(rs.getString("type")),
                rs.getObject("resource_id", UUID.class),
                deserializePayload(rs.getString("payload")),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("processed_at", OffsetDateTime.class)
        );
    }

    public NotificationOutbox insertOutboxEntry(
            NotificationType type,
            UUID resourceId,
            OutboxPayload payload
    ) {
        return jdbcClient
                .sql("""
                        INSERT INTO notification_outbox (
                            type,
                            resource_id,
                            payload
                        )
                        VALUES (
                            :type,
                            :resourceId,
                            CAST(:payload AS jsonb)
                        )
                        RETURNING
                        """ + OUTBOX_COLUMNS)
                .param("type", type.toDbValue())
                .param("resourceId", resourceId)
                .param("payload", serializePayload(payload))
                .query(outboxRowMapper())
                .single();
    }

    public List<NotificationOutbox> findPendingOutboxEntries(int limit) {
        return jdbcClient
                .sql("""
                        SELECT
                        """ + OUTBOX_COLUMNS + """
                        FROM notification_outbox
                        WHERE processed_at IS NULL
                        ORDER BY created_at ASC, id ASC
                        LIMIT :limit
                        """)
                .param("limit", limit)
                .query(outboxRowMapper())
                .list();
    }

    public boolean markOutboxProcessed(UUID id) {
        return jdbcClient
                .sql("""
                        UPDATE notification_outbox
                        SET processed_at = now()
                        WHERE id = :id
                          AND processed_at IS NULL
                        """)
                .param("id", id)
                .update() == 1;
    }

    public Optional<UUID> findFeedbackIdBySubmission(
            Topic topic,
            String eventTitle,
            int rating,
            String message,
            String senderName,
            String senderEmail
    ) {
        return jdbcClient
                .sql("""
                        SELECT id
                        FROM feedbacks
                        WHERE topic = :topic
                          AND rating = :rating
                          AND message IS NOT DISTINCT FROM :message
                          AND sender_name IS NOT DISTINCT FROM :senderName
                          AND sender_email = :senderEmail
                          AND event_title IS NOT DISTINCT FROM :eventTitle
                        ORDER BY created_at DESC, id DESC
                        LIMIT 1
                        """)
                .param("topic", topic.toDbValue())
                .param("eventTitle", eventTitle)
                .param("rating", rating)
                .param("message", message)
                .param("senderName", senderName)
                .param("senderEmail", senderEmail)
                .query((rs, _) -> rs.getObject("id", UUID.class))
                .optional();
    }

    public boolean feedbackExists(UUID id) {
        return jdbcClient
                .sql("""
                        SELECT EXISTS(
                            SELECT 1
                            FROM feedbacks
                            WHERE id = :id
                        )
                        """)
                .param("id", id)
                .query(Boolean.class)
                .single();
    }

    private String serializePayload(OutboxPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize outbox payload", exception);
        }
    }

    private OutboxPayload deserializePayload(String json) {
        try {
            return objectMapper.readValue(json, OutboxPayload.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to deserialize outbox payload", exception);
        }
    }
}