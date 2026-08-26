package nl.hackyourfuture.project.backend.event.comment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import nl.hackyourfuture.project.backend.event.comment.model.EventComment;

import java.time.OffsetDateTime;
import java.util.UUID;


@Schema(description = "A comment on an event with an optional admin reply")
public record EventCommentResponse(

        @Schema(
                description = "Unique identifier of the comment",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID id,

        @Schema(
                description = "Identifier of the related event",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID eventId,

        @Schema(
                description = "Name of the user who posted the comment",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String userName,

        @Schema(
                description = "Comment text",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String content,

        @Schema(
                description = "When the comment was created",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        OffsetDateTime createdAt,

        @Schema(
                description = "When the comment was last updated",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        OffsetDateTime updatedAt,

        @Schema(
                description = "Admin reply; null when there is no reply",
                nullable = true
        )
        String adminReply,

        @Schema(
                description = "When the admin reply was created",
                nullable = true
        )
        OffsetDateTime adminReplyCreatedAt,

        @Schema(
                description = "When the admin reply was last updated",
                nullable = true
        )
        OffsetDateTime adminReplyUpdatedAt

) {

    public static EventCommentResponse from(EventComment comment) {
        return new EventCommentResponse(
                comment.id(),
                comment.eventId(),
                comment.userName(),
                comment.content(),
                comment.createdAt(),
                comment.updatedAt(),
                comment.adminReply(),
                comment.adminReplyCreatedAt(),
                comment.adminReplyUpdatedAt()
        );
    }
}
