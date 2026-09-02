package nl.hackyourfuture.project.backend.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "The conversation so far, sent with each new question")
public record ChatRequest(

    @Schema(
        description = "Full conversation history, in order — includes the new question as the last message",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<ChatMessageDto> messages
) {
}
