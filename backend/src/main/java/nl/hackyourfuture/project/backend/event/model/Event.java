package nl.hackyourfuture.project.backend.event.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Event(UUID id,
                    String title,
                    String description,
                    UUID categoryId,
                    UUID addressId,
                    OffsetDateTime startAt,
                    OffsetDateTime endAt,
                    BigDecimal price,
                    UUID createdByUserId,
                    boolean cancelled,
                    OffsetDateTime createdAt,
                    OffsetDateTime updatedAt) {
}
