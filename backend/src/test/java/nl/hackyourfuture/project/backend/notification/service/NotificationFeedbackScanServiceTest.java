package nl.hackyourfuture.project.backend.notification.service;

import nl.hackyourfuture.project.backend.notification.model.Notification;
import nl.hackyourfuture.project.backend.notification.model.NotificationType;
import nl.hackyourfuture.project.backend.notification.repository.FeedbackNotificationRepository;
import nl.hackyourfuture.project.backend.notification.repository.NotificationRepository;
import nl.hackyourfuture.project.backend.user.UserRepository;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationFeedbackScanServiceTest {

    @Mock
    private FeedbackNotificationRepository feedbackNotificationRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationFeedbackScanService notificationFeedbackScanService;

    @Test
    void sendNewFeedbackNotifications_whenCandidatesExist_createsNotificationForAdmin() {
        UUID adminId = UUID.randomUUID();
        UUID eventFeedbackId = UUID.randomUUID();
        UUID appFeedbackId = UUID.randomUUID();

        when(feedbackNotificationRepository.findUnnotifiedFeedbackCreatedAfter(
                any(OffsetDateTime.class),
                eq(20)
        )).thenReturn(List.of(
                new FeedbackNotificationRepository.FeedbackNotificationCandidate(
                        eventFeedbackId,
                        "Jazz Night"
                ),
                new FeedbackNotificationRepository.FeedbackNotificationCandidate(
                        appFeedbackId,
                        null
                )
        ));
        when(userRepository.findAdminUserId()).thenReturn(Optional.of(adminId));
        when(notificationRepository.createNotificationIfAbsent(
                eq(adminId),
                eq(NotificationType.NEW_FEEDBACK),
                eq("New feedback received"),
                eq("New feedback about Jazz Night."),
                eq(eventFeedbackId),
                eq("/admin/messages")
        )).thenReturn(Optional.of(notification(
                adminId,
                eventFeedbackId,
                "New feedback about Jazz Night."
        )));
        when(notificationRepository.createNotificationIfAbsent(
                eq(adminId),
                eq(NotificationType.NEW_FEEDBACK),
                eq("New feedback received"),
                eq("New feedback about the app."),
                eq(appFeedbackId),
                eq("/admin/messages")
        )).thenReturn(Optional.of(notification(
                adminId,
                appFeedbackId,
                "New feedback about the app."
        )));

        notificationFeedbackScanService.sendNewFeedbackNotifications();

        verify(notificationRepository).createNotificationIfAbsent(
                adminId,
                NotificationType.NEW_FEEDBACK,
                "New feedback received",
                "New feedback about Jazz Night.",
                eventFeedbackId,
                "/admin/messages"
        );
        verify(notificationRepository).createNotificationIfAbsent(
                adminId,
                NotificationType.NEW_FEEDBACK,
                "New feedback received",
                "New feedback about the app.",
                appFeedbackId,
                "/admin/messages"
        );
    }

    @Test
    void sendNewFeedbackNotifications_whenNoCandidates_doesNothing() {
        when(feedbackNotificationRepository.findUnnotifiedFeedbackCreatedAfter(
                any(OffsetDateTime.class),
                anyInt()
        )).thenReturn(List.of());

        notificationFeedbackScanService.sendNewFeedbackNotifications();

        verify(userRepository, never()).findAdminUserId();
        verify(notificationRepository, never()).createNotificationIfAbsent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void sendNewFeedbackNotifications_whenNoAdmin_doesNothing() {
        when(feedbackNotificationRepository.findUnnotifiedFeedbackCreatedAfter(
                any(OffsetDateTime.class),
                anyInt()
        )).thenReturn(List.of(
                new FeedbackNotificationRepository.FeedbackNotificationCandidate(
                        UUID.randomUUID(),
                        "Jazz Night"
                )
        ));
        when(userRepository.findAdminUserId()).thenReturn(Optional.empty());

        notificationFeedbackScanService.sendNewFeedbackNotifications();

        verify(notificationRepository, never()).createNotificationIfAbsent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    private static Notification notification(UUID adminId, UUID feedbackId, String body) {
        return new Notification(
                UUID.randomUUID(),
                adminId,
                NotificationType.NEW_FEEDBACK,
                "New feedback received",
                body,
                feedbackId,
                "/admin/messages",
                null,
                OffsetDateTime.now()
        );
    }
}
