package nl.hackyourfuture.project.backend.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "The conversation so far, sent with each new question")
public record ChatRequest(

    @NotEmpty(message = "At least one message is required")
    @Size(max = 20, message = "Conversation history is too long")
    @Valid
    @Schema(
        description = "Full conversation history, in order — includes the new question as the last message",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<ChatMessageDto> messages
) {
}
