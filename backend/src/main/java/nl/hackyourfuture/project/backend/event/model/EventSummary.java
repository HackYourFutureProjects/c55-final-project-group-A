package nl.hackyourfuture.project.backend.event.model;

import nl.hackyourfuture.project.backend.event.category.model.Category;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record EventSummary(
        UUID id,
        String title,
        List<Category> categories,
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