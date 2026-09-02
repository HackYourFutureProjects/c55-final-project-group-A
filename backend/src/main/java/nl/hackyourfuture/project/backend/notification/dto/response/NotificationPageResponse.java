package nl.hackyourfuture.project.backend.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "One page of notifications for the current user")
public record NotificationPageResponse(

        @Schema(
                description = "Notifications in the current page",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        List<NotificationResponse> notifications,

        @Schema(
                description = "Current zero-based page number",
                example = "0",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        int page,

        @Schema(
                description = "Maximum number of notifications in one page",
                example = "20",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        int size,

        @Schema(
                description = "Total number of matching notifications across all pages",
                example = "27",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        long totalElements,

        @Schema(
                description = "Total number of pages",
                example = "2",
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