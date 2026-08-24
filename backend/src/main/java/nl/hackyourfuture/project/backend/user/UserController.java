package nl.hackyourfuture.project.backend.user;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.user.dto.PatchUserRequest;
import nl.hackyourfuture.project.backend.user.dto.UserResponse;
import nl.hackyourfuture.project.backend.user.interactions.UserEventService;
import nl.hackyourfuture.project.backend.user.interactions.dto.SavedGoingEventCardPageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Operations on user accounts")
public class UserController {

  private final UserService userService;
  private final UserEventService userEventService;

  @GetMapping
  @Operation(summary = "Info about current user", description = "Returns all basic user information.")
  @ApiResponse(responseCode = "200", description = "User info")
  @ApiResponse(
      responseCode = "404",
      description = "User not found",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  public UserResponse getCurrentUser(Authentication authentication) {
    UUID id = (UUID) authentication.getPrincipal();
    return userService.getCurrentUser(id);
  }


  @PatchMapping
  @Operation(summary = "Update an existing user", description = "Update the details of the current user.")
  @ApiResponse(responseCode = "200", description = "The updated user")
  @ApiResponse(
      responseCode = "400",
      description = "The request body is invalid",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "404",
      description = "User not found",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  public UserResponse updateUser(@Valid @RequestBody PatchUserRequest request, Authentication authentication) {
    UUID id = (UUID) authentication.getPrincipal();
    return userService.updateUser(id, request);
  }


  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete the current user", description = "Permanently deletes the current user's account.")
  @ApiResponse(responseCode = "204", description = "User successfully deleted")
  @ApiResponse(
      responseCode = "404",
      description = "User not found",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  public void deleteUser(Authentication authentication) {
    UUID id = (UUID) authentication.getPrincipal();
    userService.deleteUser(id);
  }

  @GetMapping("/saved")
  @Operation(summary = "List saved events", description = "Returns a paginated list of events the current user has saved.")
  @ApiResponse(responseCode = "200", description = "A page of saved events")
  @ApiResponse(
      responseCode = "400",
      description = "Page or size is outside the allowed range",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "404",
      description = "User not found",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  public SavedGoingEventCardPageResponse getSavedEventsByCurrentUser(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "9") @Min(1) @Max(100) int size,
      Authentication authentication
  ) {
    UUID userId = (UUID) authentication.getPrincipal();
    return userEventService.getSavedEventsSummary(userId, page, size);
  }

  @GetMapping("/going")
  @Operation(summary = "List events I'm going to", description = "Returns a paginated list of events the current user marked as attending.")
  @ApiResponse(responseCode = "200", description = "A page of events the user is attending")
  @ApiResponse(
      responseCode = "400",
      description = "Page or size is outside the allowed range",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "404",
      description = "User not found",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  public SavedGoingEventCardPageResponse getGoingEventsByCurrentUser(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                              @RequestParam(defaultValue = "9") @Min(1) @Max(100) int size,
                                                              Authentication authentication) {
    UUID userId = (UUID) authentication.getPrincipal();
    return userEventService.getGoingEventsSummary(userId, page, size);
  }
}
