package nl.hackyourfuture.project.backend.event.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record NewEvent(
        String title,
        String description,
        UUID addressId,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        BigDecimal price,
        UUID createdByUserId
) {
}
