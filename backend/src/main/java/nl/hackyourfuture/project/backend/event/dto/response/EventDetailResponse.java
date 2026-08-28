package nl.hackyourfuture.project.backend.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import nl.hackyourfuture.project.backend.event.model.EventDetail;
import nl.hackyourfuture.project.backend.event.model.EventStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import nl.hackyourfuture.project.backend.event.category.dto.CategoryResponse;

import java.util.List;

@Schema(description = "Detailed public information about a single event")
public record EventDetailResponse(

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
                description = "Full description of the event",
                example = "An evening of live music in central Amsterdam",
                nullable = true
        )
        String description,

        @Schema(
                description = "Categories assigned to the event",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        List<CategoryResponse> categories,

        @Schema(
                description = "Date and time when the event starts",
                example = "2026-09-12T17:00:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        OffsetDateTime startAt,

        @Schema(
                description = "Date and time when the event ends; null when unknown",
                example = "2026-09-12T21:30:00Z",
                nullable = true,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        OffsetDateTime endAt,

        @Schema(
                description = "Event price in euros; null when unknown",
                example = "24.00",
                minimum = "0",
                nullable = true,
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
                nullable = true
        )
        String houseNumber,

        @Schema(
                description = "Postal code of the event location",
                example = "1032 KJ",
                nullable = true
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
                nullable = true
        )
        String province,

        @Schema(
                description = "Latitude of the event location",
                example = "52.367600",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        BigDecimal latitude,

        @Schema(
                description = "Longitude of the event location",
                example = "4.904100",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        BigDecimal longitude,

        @Schema(
                description = "Public URL of the event's primary image",
                example = "https://ik.imagekit.io/example/events/music-night.jpg",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String imageUrl,

        @Schema(
                description = "Number of users attending the event",
                example = "328",
                minimum = "0",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        long goingCount,

        @Schema(
                description = """
                        Current event status. Cancellation takes priority over
                        statuses calculated from the event start and end times.
                        """,
                example = "UPCOMING",
                allowableValues = {
                        "UPCOMING",
                        "ONGOING",
                        "PAST",
                        "CANCELLED"
                },
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        EventStatus eventStatus

) {

    public static EventDetailResponse from(
            EventDetail event,
            EventStatus status
    ) {
        return new EventDetailResponse(
                event.id(),
                event.title(),
                event.description(),
                event.categories()
                        .stream()
                        .map(CategoryResponse::from)
                        .toList(),
                event.startAt(),
                event.endAt(),
                event.price(),
                event.street(),
                event.houseNumber(),
                event.postalCode(),
                event.cityName(),
                event.province(),
                event.latitude(),
                event.longitude(),
                event.imageUrl(),
                event.goingCount(),
                status
        );
    }
}
