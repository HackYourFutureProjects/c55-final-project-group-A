package nl.hackyourfuture.project.backend.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The AI-generated reply")
public record ChatResponse(

    @Schema(
        description = "The assistant's answer to the latest message",
        example = "Since it's an evening outdoor event, I'd recommend a light jacket — the forecast shows 17°C and partly cloudy conditions.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String reply
) {
}
