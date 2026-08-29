package nl.hackyourfuture.project.backend.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The new review status for a feedback")
public record PatchFeedbackRequest(
    @Schema(description = "Whether the feedback has been reviewed", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean isReviewed
) {
}
