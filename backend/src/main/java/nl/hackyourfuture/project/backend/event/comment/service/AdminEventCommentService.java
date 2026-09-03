package nl.hackyourfuture.project.backend.event.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.hackyourfuture.project.backend.event.comment.dto.request.AdminReplyRequest;
import nl.hackyourfuture.project.backend.event.comment.dto.response.EventCommentResponse;
import nl.hackyourfuture.project.backend.event.comment.exceptions.AdminReplyAlreadyExistsException;
import nl.hackyourfuture.project.backend.event.comment.exceptions.AdminReplyNotFoundException;
import nl.hackyourfuture.project.backend.event.comment.exceptions.CommentNotFoundException;
import nl.hackyourfuture.project.backend.event.comment.model.EventComment;
import nl.hackyourfuture.project.backend.event.comment.repository.EventCommentRepository;
import nl.hackyourfuture.project.backend.event.exceptions.EventNotFoundException;
import nl.hackyourfuture.project.backend.event.repository.EventRepository;
import nl.hackyourfuture.project.backend.notification.model.OutboxPayload;
import nl.hackyourfuture.project.backend.notification.service.NotificationOutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEventCommentService {

    private final EventCommentRepository eventCommentRepository;
    private final NotificationOutboxService notificationOutboxService;
    private final EventRepository eventRepository;

    @Transactional
    public EventCommentResponse createAdminReply(
            UUID commentId,
            UUID adminUserId,
            AdminReplyRequest request
    ) {
        log.debug(
                "Creating admin reply for comment {} by admin {}",
                commentId,
                adminUserId
        );

        EventComment existingComment = eventCommentRepository
                .findCommentById(commentId)
                .orElseThrow(() ->
                        new CommentNotFoundException(
                                "Comment not found: " + commentId
                        )
                );

        if (existingComment.adminReply() != null) {
            throw new AdminReplyAlreadyExistsException(
                    "This comment already has an admin reply"
            );
        }

        EventComment repliedComment = eventCommentRepository
                .createAdminReply(
                        commentId,
                        adminUserId,
                        request.content().trim()
                )
                .orElseThrow(() ->
                        new AdminReplyAlreadyExistsException(
                                "This comment already has an admin reply"
                        )
                );

        UUID eventId = repliedComment.eventId();
        String eventTitle = eventRepository
                .findEventDetailById(eventId)
                .orElseThrow(() ->
                        new EventNotFoundException(
                                "Event not found: " + eventId
                        )
                )
                .title();

        log.info(
                "Created admin reply for comment {}, enqueuing COMMENT_REPLY notification",
                commentId
        );
        notificationOutboxService.enqueueCommentReply(
                commentId,
                new OutboxPayload(
                        eventTitle,
                        "/events/" + eventId
                )
        );

        log.debug("Created admin reply for comment {}", commentId);

        return EventCommentResponse.from(repliedComment);
    }

    @Transactional
    public EventCommentResponse updateAdminReply(
            UUID commentId,
            AdminReplyRequest request
    ) {
        log.debug("Updating admin reply for comment {}", commentId);

        EventComment existingComment = eventCommentRepository
                .findCommentById(commentId)
                .orElseThrow(() ->
                        new CommentNotFoundException(
                                "Comment not found: " + commentId
                        )
                );

        if (existingComment.adminReply() == null) {
            throw new AdminReplyNotFoundException(
                    "Admin reply not found for comment: " + commentId
            );
        }

        EventComment updatedComment = eventCommentRepository
                .updateAdminReply(
                        commentId,
                        request.content().trim()
                )
                .orElseThrow(() ->
                        new AdminReplyNotFoundException(
                                "Admin reply not found for comment: " + commentId
                        )
                );

        log.debug("Updated admin reply for comment {}", commentId);

        return EventCommentResponse.from(updatedComment);
    }

    @Transactional
    public void deleteAdminReply(UUID commentId) {
        log.debug("Deleting admin reply for comment {}", commentId);

        EventComment existingComment = eventCommentRepository
                .findCommentById(commentId)
                .orElseThrow(() ->
                        new CommentNotFoundException(
                                "Comment not found: " + commentId
                        )
                );

        if (existingComment.adminReply() == null) {
            throw new AdminReplyNotFoundException(
                    "Admin reply not found for comment: " + commentId
            );
        }

        boolean deleted =
                eventCommentRepository.deleteAdminReply(commentId);

        if (!deleted) {
            throw new AdminReplyNotFoundException(
                    "Admin reply not found for comment: " + commentId
            );
        }

        log.debug("Deleted admin reply for comment {}", commentId);
    }

    @Transactional
    public void deleteComment(UUID commentId) {
        log.debug("Deleting comment {}", commentId);

        boolean deleted =
                eventCommentRepository.deleteCommentById(commentId);

        if (!deleted) {
            throw new CommentNotFoundException(
                    "Comment not found: " + commentId
            );
        }

        log.debug("Deleted comment {}", commentId);
    }
}
