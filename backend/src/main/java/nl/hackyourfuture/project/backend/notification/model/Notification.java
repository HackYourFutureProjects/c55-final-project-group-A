package nl.hackyourfuture.project.backend.notification.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Notification(
        UUID id,
        UUID userId,
        NotificationType type,
        String title,
        String body,
        UUID resourceId,
        String linkPath,
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {
    public boolean isRead() {
        return readAt != null;
    }
}