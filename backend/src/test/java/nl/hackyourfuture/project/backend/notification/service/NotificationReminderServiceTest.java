package nl.hackyourfuture.project.backend.notification.service;

import nl.hackyourfuture.project.backend.notification.model.Notification;
import nl.hackyourfuture.project.backend.notification.model.NotificationType;
import nl.hackyourfuture.project.backend.notification.repository.EventReminderRepository;
import nl.hackyourfuture.project.backend.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationReminderServiceTest {

    @Mock
    private EventReminderRepository eventReminderRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationReminderService notificationReminderService;

    @Test
    void sendEventReminders_whenCandidatesExist_createsNotificationIfAbsent() {
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        String eventTitle = "Jazz Night";

        when(eventReminderRepository.findGoingUsersWithEventsStartingBetween(
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(List.of(
                new EventReminderRepository.EventReminderCandidate(userId, eventId, eventTitle)
        ));
        when(notificationRepository.createNotificationIfAbsent(
                eq(userId),
                eq(NotificationType.EVENT_REMINDER),
                eq("Starting soon"),
                eq(eventTitle + " starts in about 24 hours."),
                eq(eventId),
                eq("/events/" + eventId)
        )).thenReturn(Optional.of(new Notification(
                UUID.randomUUID(),
                userId,
                NotificationType.EVENT_REMINDER,
                "Starting soon",
                eventTitle + " starts in about 24 hours.",
                eventId,
                "/events/" + eventId,
                null,
                OffsetDateTime.now()
        )));

        notificationReminderService.sendEventReminders();

        verify(notificationRepository).createNotificationIfAbsent(
                userId,
                NotificationType.EVENT_REMINDER,
                "Starting soon",
                eventTitle + " starts in about 24 hours.",
                eventId,
                "/events/" + eventId
        );
    }

    @Test
    void sendEventReminders_whenNoCandidates_doesNothing() {
        when(eventReminderRepository.findGoingUsersWithEventsStartingBetween(
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(List.of());

        notificationReminderService.sendEventReminders();

        verify(notificationRepository, never()).createNotificationIfAbsent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }
}
