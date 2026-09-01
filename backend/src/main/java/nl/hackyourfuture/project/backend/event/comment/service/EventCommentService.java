package nl.hackyourfuture.project.backend.event.comment.service;


import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.comment.dto.request.CreateCommentRequest;
import nl.hackyourfuture.project.backend.event.comment.dto.request.UpdateCommentRequest;
import nl.hackyourfuture.project.backend.event.comment.dto.response.EventCommentPageResponse;
import nl.hackyourfuture.project.backend.event.comment.dto.response.EventCommentResponse;
import nl.hackyourfuture.project.backend.event.comment.exceptions.CommentNotFoundException;
import nl.hackyourfuture.project.backend.event.comment.model.EventComment;
import nl.hackyourfuture.project.backend.event.comment.repository.EventCommentRepository;
import nl.hackyourfuture.project.backend.event.exceptions.EventNotFoundException;
import nl.hackyourfuture.project.backend.event.repository.EventRegistryRepository;
import nl.hackyourfuture.project.backend.event.repository.EventRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventCommentService {

    private final EventCommentRepository eventCommentRepository;
    private final EventRepository eventRepository;
    private final EventRegistryRepository eventRegistryRepository;

    private void validateEventExists(UUID eventId) {
        eventRepository
                .findEventDetailById(eventId)
                .orElseThrow(() ->
                        new EventNotFoundException("Event not found: " + eventId)
                );
    }

    private EventComment getOwnedCommentOrThrow(
            UUID commentId,
            UUID userId
    ) {
        EventComment comment = eventCommentRepository
                .findCommentById(commentId)
                .orElseThrow(() ->
                        new CommentNotFoundException(
                                "Comment not found: " + commentId
                        )
                );

        if (!comment.userId().equals(userId)) {
            throw new AccessDeniedException(
                    "You can only edit or delete your own comments"
            );
        }

        return comment;
    }

    @Transactional(readOnly = true)
    public EventCommentPageResponse getComments(
            UUID eventId,
            int page,
            int size
    ) {
        validateEventExists(eventId);

        long offset = (long) page * size;

        List<EventCommentResponse> comments = eventCommentRepository
                .findCommentsByEventId(eventId, size, offset)
                .stream()
                .map(EventCommentResponse::from)
                .toList();

        long totalComments =
                eventCommentRepository.countCommentsByEventId(eventId);

        boolean hasMore =
                offset + comments.size() < totalComments;

        return new EventCommentPageResponse(
                comments,
                totalComments,
                hasMore
        );
    }

    @Transactional
    public EventCommentResponse createComment(
            UUID eventId,
            UUID userId,
            CreateCommentRequest request
    ) {
        validateEventExists(eventId);

        eventRegistryRepository.registerEventIfMissing(eventId);

        EventComment comment = eventCommentRepository.createComment(
                eventId,
                userId,
                request.content().trim()
        );

        return EventCommentResponse.from(comment);
    }

    @Transactional
    public EventCommentResponse updateComment(
            UUID commentId,
            UUID userId,
            UpdateCommentRequest request
    ) {
        getOwnedCommentOrThrow(commentId, userId);

        EventComment comment = eventCommentRepository
                .updateComment(
                        commentId,
                        userId,
                        request.content().trim()
                )
                .orElseThrow(() ->
                        new CommentNotFoundException(
                                "Comment not found: " + commentId
                        )
                );

        return EventCommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(
            UUID commentId,
            UUID userId
    ) {
        getOwnedCommentOrThrow(commentId, userId);

        boolean deleted =
                eventCommentRepository.deleteCommentById(commentId);

        if (!deleted) {
            throw new CommentNotFoundException(
                    "Comment not found: " + commentId
            );
        }
    }
}
