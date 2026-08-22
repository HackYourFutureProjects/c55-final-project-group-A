package nl.hackyourfuture.project.backend.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import nl.hackyourfuture.project.backend.event.category.dto.CategoryResponse;
import nl.hackyourfuture.project.backend.event.model.AdminEventDetail;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(
        description = """
                Complete event information returned to an administrator.
                Unlike the public event response, this response can include
                unpublished drafts.
                """
)
public record AdminEventDetailResponse(

        @Schema(
                description = "Unique identifier of the event",
                example = "40000000-0000-0000-0000-000000000001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID id,

        @Schema(
                description = "Event title",
                example = "Synth Night: Analog Futures",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String title,

        @Schema(
                description = "Full event description",
                example = "Two rooms, four acts, and live visuals.",
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
                example = "2026-09-13T21:00:00+02:00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        OffsetDateTime startAt,

        @Schema(
                description = "Date and time when the event ends",
                example = "2026-09-14T03:00:00+02:00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        OffsetDateTime endAt,

        @Schema(
                description = "Event price in euros",
                example = "22.00",
                minimum = "0",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        BigDecimal price,

        @Schema(
                description = "Street where the event takes place",
                example = "Weteringschans",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String street,

        @Schema(
                description = "House or building number",
                example = "6",
                nullable = true
        )
        String houseNumber,

        @Schema(
                description = "Postal code of the event location",
                example = "1017SG",
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
                description = "Province or region",
                example = "North Holland",
                nullable = true
        )
        String province,

        @Schema(
                description = "Latitude of the event location",
                example = "52.3612",
                minimum = "-90",
                maximum = "90",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        BigDecimal latitude,

        @Schema(
                description = "Longitude of the event location",
                example = "4.8828",
                minimum = "-180",
                maximum = "180",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        BigDecimal longitude,

        @Schema(
                description = "Primary event image URL",
                example = "https://ik.imagekit.io/example/events/event.jpg",
                nullable = true
        )
        String imageUrl,

        @Schema(
                description = "Number of users who marked the event as going",
                example = "42",
                minimum = "0",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        long goingCount,

        @Schema(
                description = "Whether the event is publicly visible",
                example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean isPublished,

        @Schema(
                description = "Whether the event has been cancelled",
                example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean cancelled
) {

    public static AdminEventDetailResponse from(AdminEventDetail event) {
        return new AdminEventDetailResponse(
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
                event.published(),
                event.cancelled()
        );
    }
}
