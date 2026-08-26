package nl.hackyourfuture.project.backend.event.comment.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventComment(
        UUID id,
        UUID eventId,
        UUID userId,
        String userName,
        String content,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String adminReply,
        UUID adminReplyByUserId,
        OffsetDateTime adminReplyCreatedAt,
        OffsetDateTime adminReplyUpdatedAt
) {
}
