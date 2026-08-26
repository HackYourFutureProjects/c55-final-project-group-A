package nl.hackyourfuture.project.backend.event.comment.service;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.comment.dto.request.AdminReplyRequest;
import nl.hackyourfuture.project.backend.event.comment.dto.response.EventCommentResponse;
import nl.hackyourfuture.project.backend.event.comment.exceptions.AdminReplyAlreadyExistsException;
import nl.hackyourfuture.project.backend.event.comment.exceptions.AdminReplyNotFoundException;
import nl.hackyourfuture.project.backend.event.comment.exceptions.CommentNotFoundException;
import nl.hackyourfuture.project.backend.event.comment.model.EventComment;
import nl.hackyourfuture.project.backend.event.comment.repository.EventCommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminEventCommentService {

    private final EventCommentRepository eventCommentRepository;
    
    @Transactional
    public EventCommentResponse createAdminReply(
            UUID commentId,
            UUID adminUserId,
            AdminReplyRequest request
    ) {
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

        return EventCommentResponse.from(repliedComment);
    }

    @Transactional
    public EventCommentResponse updateAdminReply(
            UUID commentId,
            AdminReplyRequest request
    ) {
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

        return EventCommentResponse.from(updatedComment);
    }

    @Transactional
    public void deleteAdminReply(UUID commentId) {
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
    }

    @Transactional
    public void deleteComment(UUID commentId) {
        boolean deleted =
                eventCommentRepository.deleteCommentById(commentId);

        if (!deleted) {
            throw new CommentNotFoundException(
                    "Comment not found: " + commentId
            );
        }
    }
}
