package nl.hackyourfuture.project.backend.event.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Fields needed to update an existing comment")
public record UpdateCommentRequest(

        @NotBlank(message = "Comment cannot be empty")
        @Size(
                max = 500,
                message = "Comment cannot exceed 500 characters"
        )
        @Schema(
                description = "Updated comment text",
                example = "Is this event suitable for beginners?",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String content
) {
}