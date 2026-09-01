package nl.hackyourfuture.project.backend.notification.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationOutbox(
        UUID id,
        NotificationType type,
        UUID resourceId,
        OutboxPayload payload,
        OffsetDateTime createdAt,
        OffsetDateTime processedAt
) {
    public boolean isPending() {
        return processedAt == null;
    }
}