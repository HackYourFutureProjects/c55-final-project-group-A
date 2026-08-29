package nl.hackyourfuture.project.backend.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "A page of feedback, with pagination metadata")
public record FeedbackPageResponse(
    @Schema(description = "List of feedbacks on this page", requiredMode = Schema.RequiredMode.REQUIRED)
    List<FeedbackResponse> feedbacks,

    @Schema(description = "Zero-based page number", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    int page,

    @Schema(description = "Number of feedbacks per page", example = "9", requiredMode = Schema.RequiredMode.REQUIRED)
    int size,

    @Schema(description = "Total number of matching feedbacks across all pages", example = "23", requiredMode = Schema.RequiredMode.REQUIRED)
    long totalElements,

    @Schema(description = "Total number of pages", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    int totalPages,

    @Schema(description = "Whether there is a next page", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean hasNext
) {
}
