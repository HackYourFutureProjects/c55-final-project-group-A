package nl.hackyourfuture.project.backend.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.hackyourfuture.project.backend.notification.model.NotificationType;
import nl.hackyourfuture.project.backend.notification.repository.FeedbackNotificationRepository;
import nl.hackyourfuture.project.backend.notification.repository.NotificationRepository;
import nl.hackyourfuture.project.backend.user.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationFeedbackScanService {

    private static final long FEEDBACK_SCAN_DELAY_MS = 10_000L;
    private static final int LOOKBACK_HOURS = 24;
    private static final int BATCH_SIZE = 20;
    private static final String ADMIN_FEEDBACK_LINK_PATH = "/admin/messages";

    private final FeedbackNotificationRepository feedbackNotificationRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Scheduled(fixedDelay = FEEDBACK_SCAN_DELAY_MS)
    @Transactional
    public void sendNewFeedbackNotifications() {
        OffsetDateTime createdAfter = OffsetDateTime.now().minusHours(LOOKBACK_HOURS);

        var candidates = feedbackNotificationRepository.findUnnotifiedFeedbackCreatedAfter(
                createdAfter,
                BATCH_SIZE
        );

        if (candidates.isEmpty()) {
            log.debug(
                    "NEW_FEEDBACK scan found nothing created after {}",
                    createdAfter
            );
            return;
        }

        Optional<UUID> adminId = userRepository.findAdminUserId();

        if (adminId.isEmpty()) {
            log.warn("Skipping NEW_FEEDBACK scan because no admin user was found");
            return;
        }

        int insertedCount = 0;

        for (var candidate : candidates) {
            String body = candidate.eventTitle() != null
                    ? "New feedback about " + candidate.eventTitle() + "."
                    : "New feedback about the app.";

            boolean inserted = notificationRepository.createNotificationIfAbsent(
                    adminId.get(),
                    NotificationType.NEW_FEEDBACK,
                    "New feedback received",
                    body,
                    candidate.feedbackId(),
                    ADMIN_FEEDBACK_LINK_PATH
            ).isPresent();

            if (inserted) {
                insertedCount++;
            }
        }

        log.info(
                "NEW_FEEDBACK scan found {} recent unnotified feedbacks, inserted {}",
                candidates.size(),
                insertedCount
        );
    }
}
