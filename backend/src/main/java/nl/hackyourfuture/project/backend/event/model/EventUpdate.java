package nl.hackyourfuture.project.backend.event.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EventUpdate(
        UUID id,
        String title,
        String description,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        BigDecimal price
) {
}