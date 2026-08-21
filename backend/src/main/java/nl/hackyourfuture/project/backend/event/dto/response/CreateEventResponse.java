package nl.hackyourfuture.project.backend.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Information returned after an admin creates an event")
public record CreateEventResponse(
        @Schema(
                description = "Unique identifier of the event",
                example = "40000000-0000-0000-0000-000000000001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID id,

        @Schema(
                description = "Whether the new event was published immediately or saved as a draft",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean isPublished,

        @Schema(
                description = "Uploaded event image URL",
                example = "https://ik.imagekit.io/example/events/event.jpg",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String imageUrl
) {
}
