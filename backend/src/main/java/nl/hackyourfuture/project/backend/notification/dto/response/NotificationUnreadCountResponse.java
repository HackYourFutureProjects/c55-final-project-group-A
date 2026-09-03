package nl.hackyourfuture.project.backend.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Unread notification count for the current user")
public record NotificationUnreadCountResponse(

        @Schema(
                description = "Number of unread notifications",
                example = "3",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        long count
) {
}
