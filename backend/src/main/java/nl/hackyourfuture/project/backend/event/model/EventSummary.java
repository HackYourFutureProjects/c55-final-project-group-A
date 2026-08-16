package nl.hackyourfuture.project.backend.event.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EventSummary(
        UUID id,
        String title,
        String categoryName,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        BigDecimal price,
        String street,
        String houseNumber,
        String postalCode,
        String cityName,
        String province,
        String imageUrl,
        long goingCount,
        boolean cancelled
) {
}