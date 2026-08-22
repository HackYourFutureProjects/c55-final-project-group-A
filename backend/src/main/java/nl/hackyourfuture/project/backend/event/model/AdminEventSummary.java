package nl.hackyourfuture.project.backend.event.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminEventSummary(
        UUID id,
        String title,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String cityName,
        String imageUrl,
        boolean published,
        boolean cancelled
) {
}