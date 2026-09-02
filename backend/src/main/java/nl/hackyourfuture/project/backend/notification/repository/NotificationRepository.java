package nl.hackyourfuture.project.backend.notification.repository;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.notification.model.Notification;
import nl.hackyourfuture.project.backend.notification.model.NotificationType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class NotificationRepository {

    private final JdbcClient jdbcClient;

    private static final String NOTIFICATION_COLUMNS = """
            id,
            user_id,
            type,
            title,
            body,
            resource_id,
            link_path,
            read_at,
            created_at
            """;

    private static final String INSERT_NOTIFICATION_SQL = """
            INSERT INTO notifications (
                user_id,
                type,
                title,
                body,
                resource_id,
                link_path
            )
            VALUES (
                :userId,
                :type,
                :title,
                :body,
                :resourceId,
                :linkPath
            )
            """;

    private static final String ON_CONFLICT_ONCE_EVER = """
            ON CONFLICT (user_id, type, resource_id)
            WHERE type IN (
                'EVENT_CANCELLED',
                'EVENT_REMINDER',
                'COMMENT_REPLY',
                'NEW_FEEDBACK'
            )
            DO NOTHING
            """;

    private static final String ON_CONFLICT_EVENT_UPDATED_UNREAD = """
            ON CONFLICT (user_id, type, resource_id)
            WHERE type = 'EVENT_UPDATED' AND read_at IS NULL
            DO NOTHING
            """;

    public static final RowMapper<Notification> NOTIFICATION_ROW_MAPPER = (rs, _) ->
            new Notification(
                    rs.getObject("id", UUID.class),
                    rs.getObject("user_id", UUID.class),
                    NotificationType.fromDbValue(rs.getString("type")),
                    rs.getString("title"),
                    rs.getString("body"),
                    rs.getObject("resource_id", UUID.class),
                    rs.getString("link_path"),
                    rs.getObject("read_at", OffsetDateTime.class),
                    rs.getObject("created_at", OffsetDateTime.class)
            );

    public Notification createNotification(
            UUID userId,
            NotificationType type,
            String title,
            String body,
            UUID resourceId,
            String linkPath
    ) {
        return jdbcClient
                .sql(INSERT_NOTIFICATION_SQL + """
                        RETURNING
                        """ + NOTIFICATION_COLUMNS)
                .param("userId", userId)
                .param("type", type.toDbValue())
                .param("title", title)
                .param("body", body)
                .param("resourceId", resourceId)
                .param("linkPath", linkPath)
                .query(NOTIFICATION_ROW_MAPPER)
                .single();
    }

    public Optional<Notification> createNotificationIfAbsent(
            UUID userId,
            NotificationType type,
            String title,
            String body,
            UUID resourceId,
            String linkPath
    ) {
        String onConflictClause = type == NotificationType.EVENT_UPDATED
                ? ON_CONFLICT_EVENT_UPDATED_UNREAD
                : ON_CONFLICT_ONCE_EVER;

        return jdbcClient
                .sql(INSERT_NOTIFICATION_SQL + onConflictClause + """
                        RETURNING
                        """ + NOTIFICATION_COLUMNS)
                .param("userId", userId)
                .param("type", type.toDbValue())
                .param("title", title)
                .param("body", body)
                .param("resourceId", resourceId)
                .param("linkPath", linkPath)
                .query(NOTIFICATION_ROW_MAPPER)
                .optional();
    }

    private static String selectNotificationFrom() {
        return """
                SELECT
                """ + NOTIFICATION_COLUMNS + """
                FROM notifications
                """;
    }

    private static String unreadFilterClause(boolean unreadOnly) {
        return unreadOnly ? "AND read_at IS NULL\n" : "";
    }

    public List<Notification> findNotificationsByUserId(
            UUID userId,
            int limit,
            int offset,
            boolean unreadOnly
    ) {
        String sql = selectNotificationFrom() + """
                WHERE user_id = :userId
                """ + unreadFilterClause(unreadOnly) + """
                ORDER BY created_at DESC, id DESC
                LIMIT :limit
                OFFSET :offset
                """;

        return jdbcClient
                .sql(sql)
                .param("userId", userId)
                .param("limit", limit)
                .param("offset", offset)
                .query(NOTIFICATION_ROW_MAPPER)
                .list();
    }

    public long countNotificationsByUserId(UUID userId, boolean unreadOnly) {
        String sql = """
                SELECT COUNT(*)
                FROM notifications
                WHERE user_id = :userId
                """ + unreadFilterClause(unreadOnly);

        return jdbcClient
                .sql(sql)
                .param("userId", userId)
                .query(Long.class)
                .single();
    }
}
