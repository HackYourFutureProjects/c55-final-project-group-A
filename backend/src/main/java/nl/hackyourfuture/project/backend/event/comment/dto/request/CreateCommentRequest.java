package nl.hackyourfuture.project.backend.event.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Details needed to create a comment on an event")
public record CreateCommentRequest(

        @NotBlank(message = "Comment cannot be empty")
        @Size(
                max = 500,
                message = "Comment cannot exceed 500 characters"
        )
        @Schema(
                description = "The comment text",
                example = "Is this event suitable for beginners?",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String content
) {
}