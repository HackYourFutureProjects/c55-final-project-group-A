package nl.hackyourfuture.project.backend.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import nl.hackyourfuture.project.backend.event.model.EventSummary;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "An event as returned by the API")
public record EventSummaryResponse(

        @Schema(
                description = "Unique identifier of the event",
                example = "40000000-0000-0000-0000-000000000001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID id,

        @Schema(
                description = "Title of the event",
                example = "Amsterdam Music Night",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String title,

        @Schema(
                description = "Name of the event category",
                example = "Music",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String categoryName,

        @Schema(
                description = "Date and time when the event starts",
                example = "2026-09-12T17:00:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        OffsetDateTime startAt,

        @Schema(
                description = "Date and time when the event ends",
                example = "2026-09-12T21:30:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        OffsetDateTime endAt,

        @Schema(
                description = "Event price in euros",
                example = "24.00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        BigDecimal price,

        @Schema(
                description = "Street where the event takes place",
                example = "Papaverweg",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String street,

        @Schema(
                description = "Building or house number",
                example = "40",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String houseNumber,

        @Schema(
                description = "Postal code of the event location",
                example = "1032 KJ",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String postalCode,

        @Schema(
                description = "City where the event takes place",
                example = "Amsterdam",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String cityName,

        @Schema(
                description = "Province where the event takes place",
                example = "North Holland",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String province,

        @Schema(
                description = "URL of the event's primary image",
                example = "https://example.com/images/music-night.jpg"
        )
        String imageUrl,

        @Schema(
                description = "Number of users attending the event",
                example = "328",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        long goingCount,

        @Schema(
                description = "Whether the event has been cancelled by admin",
                example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean cancelled

) {

    public static EventSummaryResponse from(EventSummary event) {
        return new EventSummaryResponse(
                event.id(),
                event.title(),
                event.categoryName(),
                event.startAt(),
                event.endAt(),
                event.price(),
                event.street(),
                event.houseNumber(),
                event.postalCode(),
                event.cityName(),
                event.province(),
                event.imageUrl(),
                event.goingCount(),
                event.cancelled()
        );
    }
}