package nl.hackyourfuture.project.backend.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single message in the conversation")
public record ChatMessageDto(
    @Schema(
        description = "Who sent this message — 'user' for the visitor, 'assistant' for a previous AI reply",
        example = "user",
        allowableValues = {"user", "assistant"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String role,

    @Schema(
        description = "The message text",
        example = "What should I wear to this event?",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String message
) {
}
