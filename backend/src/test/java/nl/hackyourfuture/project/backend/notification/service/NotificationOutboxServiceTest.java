package nl.hackyourfuture.project.backend.notification.service;

import nl.hackyourfuture.project.backend.event.comment.repository.EventCommentRepository;
import nl.hackyourfuture.project.backend.notification.model.NotificationOutbox;
import nl.hackyourfuture.project.backend.notification.model.NotificationType;
import nl.hackyourfuture.project.backend.notification.model.OutboxPayload;
import nl.hackyourfuture.project.backend.notification.repository.NotificationOutboxRepository;
import nl.hackyourfuture.project.backend.notification.repository.NotificationRepository;
import nl.hackyourfuture.project.backend.user.interactions.UserEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxServiceTest {

    @Mock
    private NotificationOutboxRepository notificationOutboxRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserEventRepository userEventRepository;

    @Mock
    private EventCommentRepository eventCommentRepository;

    @InjectMocks
    private NotificationOutboxService notificationOutboxService;

    @Test
    void processPendingOutboxEntries_whenEventCancelled_notifiesInterestedUsers() {
        UUID outboxId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID interestedUserId = UUID.randomUUID();
        OutboxPayload payload = new OutboxPayload("City Festival", "/events/" + eventId);
        NotificationOutbox entry = new NotificationOutbox(
                outboxId,
                NotificationType.EVENT_CANCELLED,
                eventId,
                payload,
                OffsetDateTime.now(),
                null
        );

        when(notificationOutboxRepository.findPendingOutboxEntries(10))
                .thenReturn(List.of(entry));
        when(userEventRepository.findUserIdsInterestedInEvent(eventId))
                .thenReturn(List.of(interestedUserId));
        when(notificationRepository.createNotificationIfAbsent(
                eq(interestedUserId),
                eq(NotificationType.EVENT_CANCELLED),
                eq("Event cancelled"),
                eq("City Festival has been cancelled."),
                eq(eventId),
                eq(payload.linkPath())
        )).thenReturn(Optional.empty());

        notificationOutboxService.processPendingOutboxEntries();

        verify(notificationRepository).createNotificationIfAbsent(
                interestedUserId,
                NotificationType.EVENT_CANCELLED,
                "Event cancelled",
                "City Festival has been cancelled.",
                eventId,
                payload.linkPath()
        );
        verify(notificationOutboxRepository).markOutboxProcessed(outboxId);
    }
}
