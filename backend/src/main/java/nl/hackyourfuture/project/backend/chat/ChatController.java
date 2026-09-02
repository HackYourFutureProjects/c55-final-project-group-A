package nl.hackyourfuture.project.backend.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.chat.dto.ChatRequest;
import nl.hackyourfuture.project.backend.chat.dto.ChatResponse;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/events/{eventId}/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "AI-powered Q&A about a specific event")
public class ChatController {

  private final ChatService chatService;

  @PostMapping
  @Operation(
      summary = "Ask about an event",
      description = "Sends the conversation history and gets an AI-generated answer grounded in this event's details."
  )
  @ApiResponse(responseCode = "200", description = "AI-generated reply")
  @ApiResponse(
      responseCode = "400",
      description = "The request body is invalid",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "404",
      description = "Event not found",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "503",
      description = "Chat is temporarily unavailable",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  public ChatResponse askAboutEvent(
      @Parameter(description = "ID of the event to ask about", example = "40000000-0000-0000-0000-000000000001")
      @PathVariable UUID eventId,
      @Valid @RequestBody ChatRequest request
  ) {
    return chatService.askAboutEvent(eventId, request);
  }

}
