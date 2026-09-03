package nl.hackyourfuture.project.backend.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.hackyourfuture.project.backend.notification.model.NotificationType;
import nl.hackyourfuture.project.backend.notification.repository.EventReminderRepository;
import nl.hackyourfuture.project.backend.notification.repository.NotificationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationReminderService {

    private static final long REMINDER_SCAN_DELAY_MS = 15 * 60 * 1000L;
    private static final int REMINDER_WINDOW_START_HOURS = 23;
    private static final int REMINDER_WINDOW_END_HOURS = 25;

    private final EventReminderRepository eventReminderRepository;
    private final NotificationRepository notificationRepository;

    @Scheduled(fixedDelay = REMINDER_SCAN_DELAY_MS)
    @Transactional
    public void sendEventReminders() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime windowStart = now.plusHours(REMINDER_WINDOW_START_HOURS);
        OffsetDateTime windowEnd = now.plusHours(REMINDER_WINDOW_END_HOURS);

        List<EventReminderRepository.EventReminderCandidate> candidates =
                eventReminderRepository.findGoingUsersWithEventsStartingBetween(
                        windowStart,
                        windowEnd
                );

        if (candidates.isEmpty()) {
            return;
        }

        int insertedCount = 0;

        for (EventReminderRepository.EventReminderCandidate candidate : candidates) {
            boolean inserted = notificationRepository.createNotificationIfAbsent(
                    candidate.userId(),
                    NotificationType.EVENT_REMINDER,
                    "Starting soon",
                    candidate.eventTitle() + " starts in about 24 hours.",
                    candidate.eventId(),
                    "/events/" + candidate.eventId()
            ).isPresent();

            if (inserted) {
                insertedCount++;
            }
        }

        log.info(
                "EVENT_REMINDER scan found {} going users in the 23-25h window, inserted {} notifications",
                candidates.size(),
                insertedCount
        );
    }

    public int deleteRemindersForEvent(UUID eventId) {
        return notificationRepository.deleteRemindersByEventId(eventId);
    }
}
