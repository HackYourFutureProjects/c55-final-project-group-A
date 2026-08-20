package nl.hackyourfuture.project.backend.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Schema(description = "The details an admin provides to create an event draft")
public record CreateEventRequest(

        @NotBlank(message = "Please provide an event title")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        @Schema(description = "Title of the event", example = "Synth Night: Analog Futures")
        String title,

        @Schema(
                description = "Optional full description of the event",
                example = "Two rooms, four acts, live visuals..."
        )
        String description,

        @NotEmpty(message = "Please provide at least one category")
        @Schema(
                description = "Categories assigned to the event",
                example = "[\"40000000-0000-0000-0000-000000000001\"]"
        )
        Set<UUID> categoryIds,

        @NotNull(message = "Please provide an address")
        @Schema(
                description = "ID of the address where the event takes place",
                example = "50000000-0000-0000-0000-000000000001"
        )
        UUID addressId,

        @NotNull(message = "Please provide a start date and time")
        @Schema(
                description = "Date and time when the event starts",
                example = "2026-09-13T21:00:00+02:00"
        )
        OffsetDateTime startAt,

        @NotNull(message = "Please provide an end date and time")
        @Schema(
                description = "Date and time when the event ends",
                example = "2026-09-14T03:00:00+02:00"
        )
        OffsetDateTime endAt,

        @NotNull(message = "Please provide an event price")
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "Price must have at most two decimal places")
        @Schema(description = "Event price in euros; use 0 for a free event", example = "22.00")
        BigDecimal price
) {
}
