package nl.hackyourfuture.project.backend.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "The details needed to create an event")
public record CreateEventRequest(
        @NotBlank(message = "Please provide title")
        @Size(max = 255, message = "The title cannot exceed 255 characters")
        @Schema(
                description = "Title of the event",
                example = "Amsterdam Music Festival"
        )
        String title,

        @Schema(
                description = "Detailed description of the event",
                example = "An outdoor music festival in central Amsterdam"
        )
        String description,

        @NotNull(message = "Please provide a category")
        @Schema(
                description = "ID of the event category",
                example = "cf3d47d1-0a47-42bb-b516-b6fc72014abb"
        )
        UUID categoryId,

        @NotNull(message = "Please provide an address")
        @Schema(
                description = "ID of the event address",
                example = "5bf0aee7-ad74-49b7-8248-63c0d648b130"
        )
        UUID addressId,

        @NotNull(message = "provide the event start date and time")
        @Schema(
                description = "Date and time when the event starts",
                example = "2026-09-20T18:00:00+02:00"
        )
        OffsetDateTime startAt,

        @NotNull(message = "Please provide the event end date and time")
        @Schema(
                description = "Date and time when the event ends",
                example = "2026-09-20T23:00:00+02:00"
        )
        OffsetDateTime endAt,

        @NotNull
        @DecimalMin(value = "0.00", message = "The price cannot be negative")
        @Schema(
                description = "Event price in euros; use 0.00 for a free event",
                example = "25.50"
        )
        BigDecimal price
) {
}