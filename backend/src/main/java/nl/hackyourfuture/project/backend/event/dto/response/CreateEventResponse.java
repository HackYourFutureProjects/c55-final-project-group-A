package nl.hackyourfuture.project.backend.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Information returned after an admin creates an event draft")
public record CreateEventResponse(
        @Schema(
                description = "Unique identifier of the event",
                example = "40000000-0000-0000-0000-000000000001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID id,

        @Schema(
                description = "Whether the event is publicly visible. Newly created events are drafts and return false.",
                example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean isPublished
) {
}
