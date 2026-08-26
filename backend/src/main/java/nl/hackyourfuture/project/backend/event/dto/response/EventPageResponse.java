package nl.hackyourfuture.project.backend.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "One page of public events")
public record EventPageResponse(

        @Schema(
                description = "Events in the current page",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        List<EventSummaryResponse> events,

        @Schema(
                description = "Current zero-based page number",
                example = "0",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        int page,

        @Schema(
                description = "Maximum number of events in one page",
                example = "10",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        int size,

        @Schema(
                description = "Total number of matching public events",
                example = "27",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        long totalElements,

        @Schema(
                description = "Total number of pages",
                example = "3",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        int totalPages,

        @Schema(
                description = "Whether another page is available",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean hasNext
) {
}