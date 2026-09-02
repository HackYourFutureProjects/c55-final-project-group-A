package nl.hackyourfuture.project.backend;

import nl.hackyourfuture.project.backend.event.comment.dto.request.AdminReplyRequest;
import nl.hackyourfuture.project.backend.event.comment.dto.response.EventCommentResponse;
import nl.hackyourfuture.project.backend.event.comment.model.EventComment;
import nl.hackyourfuture.project.backend.event.comment.repository.EventCommentRepository;
import nl.hackyourfuture.project.backend.event.comment.service.AdminEventCommentService;
import nl.hackyourfuture.project.backend.event.repository.EventRegistryRepository;
import nl.hackyourfuture.project.backend.notification.model.Notification;
import nl.hackyourfuture.project.backend.notification.model.NotificationType;
import nl.hackyourfuture.project.backend.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class NotificationScopeIntegrationTests {

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EventRegistryRepository eventRegistryRepository;

    @Autowired
    private EventCommentRepository eventCommentRepository;

    @Autowired
    private AdminEventCommentService adminEventCommentService;

    private UUID userId;
    private UUID otherUserId;
    private UUID adminUserId;

    @BeforeEach
    void setUpUsers() {
        userId = insertUser("user");
        otherUserId = insertUser("user");
        adminUserId = jdbcClient
                .sql("SELECT id FROM users WHERE role = 'admin' LIMIT 1")
                .query(UUID.class)
                .single();
    }

    @Test
    void userCannotOpenAnotherUsersNotification() {
        Notification notification = notificationRepository.createNotification(
                userId,
                NotificationType.EVENT_CANCELLED,
                "Event cancelled",
                "An event was cancelled.",
                UUID.randomUUID(),
                "/events/example"
        );

        assertTrue(notificationRepository
                .markAsRead(notification.id(), otherUserId, false)
                .isEmpty());

        Notification stillUnread = notificationRepository
                .findNotificationByIdAndUserId(notification.id(), userId, false)
                .orElseThrow();

        assertFalse(stillUnread.isRead());
        assertTrue(notificationRepository
                .markAsRead(notification.id(), userId, false)
                .orElseThrow()
                .isRead());
    }

    @Test
    void eventUpdateRefreshesExistingUnreadNotificationWithoutDuplicatingIt() {
        UUID eventId = UUID.randomUUID();

        Notification first = notificationRepository
                .createNotificationIfAbsent(
                        userId,
                        NotificationType.EVENT_UPDATED,
                        "Event updated",
                        "Old details",
                        eventId,
                        "/events/old"
                )
                .orElseThrow();

        Notification refreshed = notificationRepository
                .createNotificationIfAbsent(
                        userId,
                        NotificationType.EVENT_UPDATED,
                        "Event updated again",
                        "New details",
                        eventId,
                        "/events/new"
                )
                .orElseThrow();

        assertEquals(first.id(), refreshed.id());
        assertEquals("Event updated again", refreshed.title());
        assertEquals("New details", refreshed.body());
        assertEquals("/events/new", refreshed.linkPath());
        assertEquals(
                1,
                notificationRepository.countNotificationsByUserId(
                        userId,
                        false,
                        false
                )
        );
    }

    @Test
    void adminReplyToTicketmasterCommentSucceedsAndEnqueuesNotification() {
        String externalEventId = "notification-test-" + UUID.randomUUID();
        String sourceUrl = "https://example.com/events/" + externalEventId;
        OffsetDateTime startAt = OffsetDateTime.now().plusDays(2);

        jdbcClient
                .sql("""
                        INSERT INTO analytics.external_events (
                            source,
                            external_event_id,
                            source_url,
                            external_venue_id,
                            start_date,
                            title,
                            category,
                            start_at,
                            is_cancelled
                        )
                        VALUES (
                            'ticketmaster',
                            :externalEventId,
                            :sourceUrl,
                            'notification-test-venue',
                            :startDate,
                            'Ticketmaster notification test',
                            'Other',
                            :startAt,
                            FALSE
                        )
                        """)
                .param("externalEventId", externalEventId)
                .param("sourceUrl", sourceUrl)
                .param("startDate", LocalDate.from(startAt))
                .param("startAt", startAt)
                .update();

        UUID eventId = jdbcClient
                .sql("SELECT id FROM event_feed WHERE source_url = :sourceUrl")
                .param("sourceUrl", sourceUrl)
                .query(UUID.class)
                .single();

        eventRegistryRepository.registerEventIfMissing(eventId);

        EventComment comment = eventCommentRepository.createComment(
                eventId,
                userId,
                "Will this external event work?"
        );

        EventCommentResponse response = adminEventCommentService.createAdminReply(
                comment.id(),
                adminUserId,
                new AdminReplyRequest("Yes, it works.")
        );

        assertEquals("Yes, it works.", response.adminReply());

        long outboxEntries = jdbcClient
                .sql("""
                        SELECT COUNT(*)
                        FROM notification_outbox
                        WHERE type = 'COMMENT_REPLY'
                          AND resource_id = :commentId
                        """)
                .param("commentId", comment.id())
                .query(Long.class)
                .single();

        assertEquals(1, outboxEntries);
    }

    private UUID insertUser(String role) {
        UUID id = UUID.randomUUID();

        return jdbcClient
                .sql("""
                        INSERT INTO users (id, email, role, name, password_hash)
                        VALUES (:id, :email, :role, 'Notification test user', 'test-password')
                        RETURNING id
                        """)
                .param("id", id)
                .param("email", id + "@example.com")
                .param("role", role)
                .query(UUID.class)
                .single();
    }
}
