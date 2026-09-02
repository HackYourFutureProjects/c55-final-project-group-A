package nl.hackyourfuture.project.backend.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "A single message in the conversation")
public record ChatMessageDto(
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "user|assistant", message = "Role must be 'user' or 'assistant'")
    @Schema(
        description = "Who sent this message — 'user' for the visitor, 'assistant' for a previous AI reply",
        example = "user",
        allowableValues = {"user", "assistant"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String role,

    @NotBlank(message = "Message content must not be empty")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    @Schema(
        description = "The message text",
        example = "What should I wear to this event?",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String message
) {
}
