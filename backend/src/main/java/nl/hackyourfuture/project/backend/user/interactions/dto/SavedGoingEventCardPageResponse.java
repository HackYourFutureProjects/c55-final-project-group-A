package nl.hackyourfuture.project.backend.user.interactions.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "A page of saved or attending events, with pagination metadata")
public record SavedGoingEventCardPageResponse(
    @Schema(description = "The events on this page", requiredMode = Schema.RequiredMode.REQUIRED)
    List<SavedGoingEventCardResponse> events,

    @Schema(description = "Zero-based page number", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    int page,

    @Schema(description = "Number of events per page", example = "9", requiredMode = Schema.RequiredMode.REQUIRED)
    int size,

    @Schema(description = "Total number of matching events across all pages", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
    long totalElements,

    @Schema(description = "Total number of pages", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    int totalPages,

    @Schema(description = "Whether there is a next page", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean hasNext
) {
}
