package nl.hackyourfuture.project.backend.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import nl.hackyourfuture.project.backend.notification.model.Notification;
import nl.hackyourfuture.project.backend.notification.model.NotificationType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "A notification shown in the user's inbox")
public record NotificationResponse(

        @Schema(
                description = "Unique identifier of the notification",
                example = "50000000-0000-0000-0000-000000000001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID id,

        @Schema(
                description = "Type of notification",
                example = "EVENT_CANCELLED",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        NotificationType type,

        @Schema(
                description = "Short title shown in the notification list",
                example = "Event cancelled",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String title,

        @Schema(
                description = "Longer message body for the notification",
                example = "Amsterdam Music Night has been cancelled.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String body,

        @Schema(
                description = "Identifier of the related resource, such as an event or comment",
                example = "40000000-0000-0000-0000-000000000001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID resourceId,

        @Schema(
                description = "Frontend path the user should navigate to when opening the notification",
                example = "/events/40000000-0000-0000-0000-000000000001",
                nullable = true
        )
        String linkPath,

        @Schema(
                description = "Whether the notification has been read",
                example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean read,

        @Schema(
                description = "When the notification was marked as read; null while unread",
                example = "2026-09-02T14:30:00Z",
                nullable = true
        )
        OffsetDateTime readAt,

        @Schema(
                description = "When the notification was created",
                example = "2026-09-02T12:00:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        OffsetDateTime createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.id(),
                notification.type(),
                notification.title(),
                notification.body(),
                notification.resourceId(),
                notification.linkPath(),
                notification.isRead(),
                notification.readAt(),
                notification.createdAt()
        );
    }
}