package nl.hackyourfuture.project.backend.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.hackyourfuture.project.backend.feedback.dto.PostFeedbackRequest;
import nl.hackyourfuture.project.backend.notification.model.NotificationOutbox;
import nl.hackyourfuture.project.backend.notification.model.NotificationType;
import nl.hackyourfuture.project.backend.notification.model.OutboxPayload;
import nl.hackyourfuture.project.backend.event.comment.model.EventComment;
import nl.hackyourfuture.project.backend.event.comment.repository.EventCommentRepository;
import nl.hackyourfuture.project.backend.notification.repository.NotificationOutboxRepository;
import nl.hackyourfuture.project.backend.notification.repository.NotificationRepository;
import nl.hackyourfuture.project.backend.user.UserRepository;
import nl.hackyourfuture.project.backend.user.interactions.UserEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutboxService {

    private static final int BATCH_SIZE = 10;
    private static final long POLL_DELAY_MS = 10_000L;
    private static final String ADMIN_FEEDBACK_LINK_PATH = "/admin/messages";

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final NotificationRepository notificationRepository;
    private final UserEventRepository userEventRepository;
    private final EventCommentRepository eventCommentRepository;
    private final UserRepository userRepository;

    public NotificationOutbox enqueueEventCancelled(UUID eventId, OutboxPayload payload) {
        log.info("Enqueueing EVENT_CANCELLED outbox entry for event {}", eventId);

        return notificationOutboxRepository.insertOutboxEntry(
                NotificationType.EVENT_CANCELLED,
                eventId,
                payload
        );
    }

    public NotificationOutbox enqueueEventUpdated(UUID eventId, OutboxPayload payload) {
        log.info("Enqueueing EVENT_UPDATED outbox entry for event {}", eventId);

        return notificationOutboxRepository.insertOutboxEntry(
                NotificationType.EVENT_UPDATED,
                eventId,
                payload
        );
    }

    public NotificationOutbox enqueueCommentReply(UUID commentId, OutboxPayload payload) {
        log.info("Enqueueing COMMENT_REPLY outbox entry for comment {}", commentId);

        return notificationOutboxRepository.insertOutboxEntry(
                NotificationType.COMMENT_REPLY,
                commentId,
                payload
        );
    }

    public NotificationOutbox enqueueNewFeedback(UUID feedbackId, OutboxPayload payload) {
        log.debug("Enqueueing NEW_FEEDBACK outbox entry for feedback {}", feedbackId);

        return notificationOutboxRepository.insertOutboxEntry(
                NotificationType.NEW_FEEDBACK,
                feedbackId,
                payload
        );
    }

    public void enqueueNewFeedbackSubmission(PostFeedbackRequest request) {
        UUID feedbackId = notificationOutboxRepository
                .findFeedbackIdBySubmission(
                        request.topic(),
                        request.eventTitle(),
                        request.rating(),
                        request.message(),
                        request.senderName(),
                        request.senderEmail()
                )
                .orElseThrow(() -> new IllegalStateException(
                        "Feedback not found after submission for sender " + request.senderEmail()
                ));

        enqueueNewFeedback(
                feedbackId,
                new OutboxPayload(request.eventTitle(), ADMIN_FEEDBACK_LINK_PATH)
        );

        log.info("Enqueued NEW_FEEDBACK notification for feedback {}", feedbackId);
    }

    @Scheduled(fixedDelay = POLL_DELAY_MS)
    @Transactional
    public void processPendingOutboxEntries() {
        List<NotificationOutbox> pendingEntries =
                notificationOutboxRepository.findPendingOutboxEntries(BATCH_SIZE);

        if (pendingEntries.isEmpty()) {
            return;
        }

        log.debug(
                "Processing {} pending notification outbox entries",
                pendingEntries.size()
        );

        for (NotificationOutbox entry : pendingEntries) {
            try {
                processEntry(entry);
            } catch (Exception exception) {
                log.warn(
                        "Failed to process outbox entry {} type={} resourceId={}",
                        entry.id(),
                        entry.type(),
                        entry.resourceId(),
                        exception
                );
            }
        }
    }

    private void processEntry(NotificationOutbox entry) {
        if (entry.type() == NotificationType.EVENT_CANCELLED) {
            processEventCancelled(entry);
            return;
        }

        if (entry.type() == NotificationType.EVENT_UPDATED) {
            processEventUpdated(entry);
            return;
        }

        if (entry.type() == NotificationType.COMMENT_REPLY) {
            processCommentReply(entry);
            return;
        }

        if (entry.type() == NotificationType.NEW_FEEDBACK) {
            processNewFeedback(entry);
            return;
        }

        log.warn(
                "Skipping unsupported outbox type {} for entry {}",
                entry.type(),
                entry.id()
        );
        notificationOutboxRepository.markOutboxProcessed(entry.id());
    }

    private void processEventCancelled(NotificationOutbox entry) {
        UUID eventId = entry.resourceId();
        OutboxPayload payload = entry.payload();

        List<UUID> userIds = userEventRepository.findUserIdsInterestedInEvent(eventId);

        String title = "Event cancelled";
        String body = payload.eventTitle() + " has been cancelled.";

        for (UUID userId : userIds) {
            notificationRepository.createNotificationIfAbsent(
                    userId,
                    NotificationType.EVENT_CANCELLED,
                    title,
                    body,
                    eventId,
                    payload.linkPath()
            );
        }

        notificationOutboxRepository.markOutboxProcessed(entry.id());

        log.info(
                "Processed EVENT_CANCELLED outbox entry {} for event {} ({} recipients)",
                entry.id(),
                eventId,
                userIds.size()
        );
    }

    private void processEventUpdated(NotificationOutbox entry) {
        UUID eventId = entry.resourceId();
        OutboxPayload payload = entry.payload();

        List<UUID> userIds = userEventRepository.findUserIdsInterestedInEvent(eventId);

        String title = "Event updated";
        String body = payload.eventTitle() + " has been updated.";

        for (UUID userId : userIds) {
            notificationRepository.createNotificationIfAbsent(
                    userId,
                    NotificationType.EVENT_UPDATED,
                    title,
                    body,
                    eventId,
                    payload.linkPath()
            );
        }

        notificationOutboxRepository.markOutboxProcessed(entry.id());

        log.info(
                "Processed EVENT_UPDATED outbox entry {} for event {} ({} recipients)",
                entry.id(),
                eventId,
                userIds.size()
        );
    }

    private void processCommentReply(NotificationOutbox entry) {
        UUID commentId = entry.resourceId();
        OutboxPayload payload = entry.payload();

        Optional<EventComment> comment =
                eventCommentRepository.findCommentById(commentId);

        if (comment.isEmpty()) {
            log.warn(
                    "Skipping COMMENT_REPLY outbox entry {} because comment {} was not found",
                    entry.id(),
                    commentId
            );
            notificationOutboxRepository.markOutboxProcessed(entry.id());
            return;
        }

        UUID userId = comment.get().userId();

        String title = "Admin replied to your comment";
        String body =
                "An admin replied to your comment on " + payload.eventTitle() + ".";

        notificationRepository.createNotificationIfAbsent(
                userId,
                NotificationType.COMMENT_REPLY,
                title,
                body,
                commentId,
                payload.linkPath()
        );

        notificationOutboxRepository.markOutboxProcessed(entry.id());

        log.info(
                "Processed COMMENT_REPLY outbox entry {} for comment {} (user {})",
                entry.id(),
                commentId,
                userId
        );
    }

    private void processNewFeedback(NotificationOutbox entry) {
        UUID feedbackId = entry.resourceId();
        OutboxPayload payload = entry.payload();

        if (!notificationOutboxRepository.feedbackExists(feedbackId)) {
            log.warn(
                    "Skipping NEW_FEEDBACK outbox entry {} because feedback {} was not found",
                    entry.id(),
                    feedbackId
            );
            notificationOutboxRepository.markOutboxProcessed(entry.id());
            return;
        }

        Optional<UUID> adminId = userRepository.findAdminUserId();

        if (adminId.isEmpty()) {
            log.warn(
                    "Skipping NEW_FEEDBACK outbox entry {} because no admin user was found",
                    entry.id()
            );
            notificationOutboxRepository.markOutboxProcessed(entry.id());
            return;
        }

        String title = "New feedback received";
        String body = payload.eventTitle() != null
                ? "New feedback about " + payload.eventTitle() + "."
                : "New feedback about the app.";

        notificationRepository.createNotificationIfAbsent(
                adminId.get(),
                NotificationType.NEW_FEEDBACK,
                title,
                body,
                feedbackId,
                payload.linkPath()
        );

        notificationOutboxRepository.markOutboxProcessed(entry.id());

        log.info(
                "Processed NEW_FEEDBACK outbox entry {} for feedback {} (admin {})",
                entry.id(),
                feedbackId,
                adminId.get()
        );
    }

}
