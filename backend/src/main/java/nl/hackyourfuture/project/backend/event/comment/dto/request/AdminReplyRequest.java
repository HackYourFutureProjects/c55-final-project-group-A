package nl.hackyourfuture.project.backend.event.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Fields needed for an admin reply to a comment")
public record AdminReplyRequest(

        @NotBlank(message = "Admin reply cannot be empty")
        @Size(
                max = 500,
                message = "Admin reply cannot exceed 500 characters"
        )
        @Schema(
                description = "The admin reply text",
                example = "Yes, beginners are welcome at this event.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String content
) {
}