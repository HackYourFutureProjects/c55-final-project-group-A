package nl.hackyourfuture.project.backend.user.interactions;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events")
public class UserEventController {

  private final UserEventService userEventService;

  @PostMapping("{eventId}/saved")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Save an event", description = "Adds the event to the current user's saved events.")
  @ApiResponse(responseCode = "204", description = "Event successfully saved")
  @ApiResponse(
      responseCode = "404",
      description = "User or event not found",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  public void addEventToSaved(@Parameter(
      description = "ID of the event to save",
      example = "effe1126-329f-4f31-942c-31bc0be4d672"
  )
                              @PathVariable UUID eventId, Authentication authentication) {
    UUID userId = (UUID) authentication.getPrincipal();
    userEventService.addEventToSaved(userId, eventId);
  }

  @PostMapping("{eventId}/going")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Mark as going", description = "Marks the current user as attending the event.")
  @ApiResponse(responseCode = "204", description = "Marked as going")
  @ApiResponse(
      responseCode = "404",
      description = "User or event not found",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  public void addEventToGoing(@Parameter(
      description = "ID of the event to mark as going",
      example = "effe1126-329f-4f31-942c-31bc0be4d672"
  )
                              @PathVariable UUID eventId, Authentication authentication) {
    UUID userId = (UUID) authentication.getPrincipal();
    userEventService.addEventToGoing(userId, eventId);
  }

  @DeleteMapping("{eventId}/saved")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Unsave an event", description = "Removes the event from the current user's saved events.")
  @ApiResponse(responseCode = "204", description = "Event successfully unsaved")
  @ApiResponse(
      responseCode = "404",
      description = "User or event not found",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  public void deleteEventFromSaved(@Parameter(
      description = "ID of the event to remove from saved",
      example = "effe1126-329f-4f31-942c-31bc0be4d672"
  )
                                   @PathVariable UUID eventId, Authentication authentication) {
    UUID userId = (UUID) authentication.getPrincipal();
    userEventService.deleteEventFromSaved(userId, eventId);
  }

  @DeleteMapping("{eventId}/going")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Unmark as going", description = "Removes the current user's 'going' status for the event.")
  @ApiResponse(responseCode = "204", description = "Going status removed")
  @ApiResponse(
      responseCode = "404",
      description = "User or event not found",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  public void deleteEventFromGoing(@Parameter(
      description = "ID of the event to unmark as going",
      example = "effe1126-329f-4f31-942c-31bc0be4d672"
  )
                                   @PathVariable UUID eventId, Authentication authentication) {
    UUID userId = (UUID) authentication.getPrincipal();
    userEventService.deleteEventFromGoing(userId, eventId);
  }
}

