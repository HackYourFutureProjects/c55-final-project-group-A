package nl.hackyourfuture.project.backend.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import nl.hackyourfuture.project.backend.event.model.AdminEventSummary;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Event information shown in the admin event list")
public record AdminEventSummaryResponse(

        @Schema(
                description = "Unique identifier of the event",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID id,

        @Schema(
                description = "Event title",
                example = "Synth Night",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String title,

        @Schema(
                description = "Event start date and time",
                example = "2026-09-13T21:00:00+02:00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        OffsetDateTime startAt,

        @Schema(
                description = "Event end date and time",
                example = "2026-09-14T03:00:00+02:00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        OffsetDateTime endAt,

        @Schema(
                description = "City where the event takes place",
                example = "Amsterdam",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String cityName,

        @Schema(
                description = "Primary event image URL",
                nullable = true
        )
        String imageUrl,

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
    public static AdminEventSummaryResponse from(AdminEventSummary event) {
        return new AdminEventSummaryResponse(
                event.id(),
                event.title(),
                event.startAt(),
                event.endAt(),
                event.cityName(),
                event.imageUrl(),
                event.published(),
                event.cancelled()
        );
    }
}
