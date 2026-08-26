package nl.hackyourfuture.project.backend.event.comment.repository;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.comment.model.EventComment;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EventCommentRepository {


    private final JdbcClient jdbcClient;

    private static final RowMapper<EventComment> EVENT_COMMENT_ROW_MAPPER =
            (rs, _) -> new EventComment(
                    rs.getObject("id", UUID.class),
                    rs.getObject("event_id", UUID.class),
                    rs.getObject("user_id", UUID.class),
                    rs.getString("user_name"),
                    rs.getString("content"),
                    rs.getObject("created_at", OffsetDateTime.class),
                    rs.getObject("updated_at", OffsetDateTime.class),
                    rs.getString("admin_reply"),
                    rs.getObject("admin_reply_by_user_id", UUID.class),
                    rs.getObject("admin_reply_created_at", OffsetDateTime.class),
                    rs.getObject("admin_reply_updated_at", OffsetDateTime.class)

            );

    private static String selectCommentWithUserFrom(String source) {
        return """
                SELECT
                    c.id,
                    c.event_id,
                    c.user_id,
                    u.name AS user_name,
                    c.content,
                    c.created_at,
                    c.updated_at,
                    c.admin_reply,
                    c.admin_reply_by_user_id,
                    c.admin_reply_created_at,
                    c.admin_reply_updated_at
                FROM %s c
                JOIN users u ON u.id = c.user_id
                """.formatted(source);
    }

    public List<EventComment> findCommentsByEventId(
            UUID eventId,
            int limit,
            long offset
    ) {
        String sql = selectCommentWithUserFrom("event_comments") + """
                WHERE c.event_id = :eventId
                ORDER BY c.created_at DESC, c.id DESC
                LIMIT :limit
                OFFSET :offset
                """;

        return jdbcClient
                .sql(sql)
                .param("eventId", eventId)
                .param("limit", limit)
                .param("offset", offset)
                .query(EVENT_COMMENT_ROW_MAPPER)
                .list();
    }

    public long countCommentsByEventId(UUID eventId) {
        String sql = """
                SELECT COUNT(*)
                FROM event_comments
                WHERE event_id = :eventId
                """;

        return jdbcClient
                .sql(sql)
                .param("eventId", eventId)
                .query(Long.class)
                .single();
    }

    public Optional<EventComment> findCommentById(UUID commentId) {
        String sql = selectCommentWithUserFrom("event_comments") + """
                WHERE c.id = :commentId
                """;

        return jdbcClient
                .sql(sql)
                .param("commentId", commentId)
                .query(EVENT_COMMENT_ROW_MAPPER)
                .optional();
    }

    public EventComment createComment(
            UUID eventId,
            UUID userId,
            String content
    ) {
        String sql = """
                WITH inserted_comment AS (
                    INSERT INTO event_comments (
                        event_id,
                        user_id,
                        content
                    )
                    VALUES (
                        :eventId,
                        :userId,
                        :content
                    )
                    RETURNING *
                )
                """ + selectCommentWithUserFrom("inserted_comment");

        return jdbcClient
                .sql(sql)
                .param("eventId", eventId)
                .param("userId", userId)
                .param("content", content)
                .query(EVENT_COMMENT_ROW_MAPPER)
                .single();
    }

    public Optional<EventComment> updateComment(
            UUID commentId,
            UUID userId,
            String content
    ) {
        String sql = """
                WITH updated_comment AS (
                    UPDATE event_comments
                    SET
                        content = :content,
                        updated_at = now()
                    WHERE id = :commentId
                      AND user_id = :userId
                    RETURNING *
                )
                """ + selectCommentWithUserFrom("updated_comment");

        return jdbcClient
                .sql(sql)
                .param("commentId", commentId)
                .param("userId", userId)
                .param("content", content)
                .query(EVENT_COMMENT_ROW_MAPPER)
                .optional();
    }

    public boolean deleteCommentById(UUID commentId) {
        String sql = """
                DELETE FROM event_comments
                WHERE id = :commentId
                """;

        return jdbcClient
                .sql(sql)
                .param("commentId", commentId)
                .update() == 1;
    }

    public Optional<EventComment> createAdminReply(
            UUID commentId,
            UUID adminUserId,
            String content
    ) {
        String sql = """
                WITH replied_comment AS (
                    UPDATE event_comments
                    SET
                        admin_reply = :content,
                        admin_reply_by_user_id = :adminUserId,
                        admin_reply_created_at = now(),
                        admin_reply_updated_at = now()
                    WHERE id = :commentId
                      AND admin_reply IS NULL
                    RETURNING *
                )
                """ + selectCommentWithUserFrom("replied_comment");

        return jdbcClient
                .sql(sql)
                .param("commentId", commentId)
                .param("adminUserId", adminUserId)
                .param("content", content)
                .query(EVENT_COMMENT_ROW_MAPPER)
                .optional();
    }

    public Optional<EventComment> updateAdminReply(
            UUID commentId,
            String content
    ) {
        String sql = """
                WITH updated_reply AS (
                    UPDATE event_comments
                    SET
                        admin_reply = :content,
                        admin_reply_updated_at = now()
                    WHERE id = :commentId
                      AND admin_reply IS NOT NULL
                    RETURNING *
                )
                """ + selectCommentWithUserFrom("updated_reply");

        return jdbcClient
                .sql(sql)
                .param("commentId", commentId)
                .param("content", content)
                .query(EVENT_COMMENT_ROW_MAPPER)
                .optional();
    }

    public boolean deleteAdminReply(UUID commentId) {
        String sql = """
                UPDATE event_comments
                SET
                    admin_reply = NULL,
                    admin_reply_by_user_id = NULL,
                    admin_reply_created_at = NULL,
                    admin_reply_updated_at = NULL
                WHERE id = :commentId
                  AND admin_reply IS NOT NULL
                """;

        return jdbcClient
                .sql(sql)
                .param("commentId", commentId)
                .update() == 1;
    }


}
