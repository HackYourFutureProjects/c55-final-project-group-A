package nl.hackyourfuture.project.backend.event.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.comment.dto.request.CreateCommentRequest;
import nl.hackyourfuture.project.backend.event.comment.dto.request.UpdateCommentRequest;
import nl.hackyourfuture.project.backend.event.comment.dto.response.EventCommentPageResponse;
import nl.hackyourfuture.project.backend.event.comment.dto.response.EventCommentResponse;
import nl.hackyourfuture.project.backend.event.comment.service.EventCommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(
        name = "Event comments",
        description = "Operations for reading, creating, editing, and deleting event comments"
)
public class EventCommentController {

    private final EventCommentService eventCommentService;

    @GetMapping("/events/{eventId}/comments")
    @SecurityRequirements
    @Operation(
            summary = "List comments for an event",
            description = """
                    Returns comments for a published event ordered from newest
                    to oldest. Comments can be read without authentication.
                    An empty comments list is returned when the event has no
                    comments.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Comments returned successfully",
            content = @Content(
                    schema = @Schema(
                            implementation = EventCommentPageResponse.class
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "The event ID, page, or size is invalid",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "The published event does not exist",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public EventCommentPageResponse getComments(
            @Parameter(
                    description = "Unique identifier of the event",
                    example = "40000000-0000-0000-0000-000000000001",
                    required = true
            )
            @PathVariable
            UUID eventId,

            @Parameter(
                    description = "Zero-based page number",
                    example = "0"
            )
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be zero or greater")
            int page,

            @Parameter(
                    description = "Number of comments per page, between 1 and 100",
                    example = "10"
            )
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not exceed 100")
            int size
    ) {
        return eventCommentService.getComments(
                eventId,
                page,
                size
        );
    }

    @PostMapping("/events/{eventId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a comment",
            description = """
                    Creates a comment on a published event for the currently
                    authenticated user. The commenter is determined from the
                    authenticated session.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Comment created successfully",
            content = @Content(
                    schema = @Schema(
                            implementation = EventCommentResponse.class
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "The event ID or comment content is invalid",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "The published event does not exist",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public EventCommentResponse createComment(
            @Parameter(
                    description = "Unique identifier of the event",
                    example = "40000000-0000-0000-0000-000000000001",
                    required = true
            )
            @PathVariable
            UUID eventId,

            @Valid
            @RequestBody
            CreateCommentRequest request,

            @Parameter(hidden = true)
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        return eventCommentService.createComment(
                eventId,
                userId,
                request
        );
    }

    @PatchMapping("/comments/{commentId}")
    @Operation(
            summary = "Update a comment",
            description = """
                    Updates a comment belonging to the currently authenticated
                    user. Users cannot edit comments created by another user.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Comment updated successfully",
            content = @Content(
                    schema = @Schema(
                            implementation = EventCommentResponse.class
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "The comment ID or updated content is invalid",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "The comment belongs to another user",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "The comment does not exist",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public EventCommentResponse updateComment(
            @Parameter(
                    description = "Unique identifier of the comment",
                    example = "50000000-0000-0000-0000-000000000001",
                    required = true
            )
            @PathVariable
            UUID commentId,

            @Valid
            @RequestBody
            UpdateCommentRequest request,

            @Parameter(hidden = true)
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        return eventCommentService.updateComment(
                commentId,
                userId,
                request
        );
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete a comment",
            description = """
                    Permanently deletes a comment belonging to the currently
                    authenticated user. Users cannot delete comments created
                    by another user.
                    """
    )
    @ApiResponse(
            responseCode = "204",
            description = "Comment deleted successfully"
    )
    @ApiResponse(
            responseCode = "400",
            description = "The supplied comment ID is not a valid UUID",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "The comment belongs to another user",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "The comment does not exist",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public void deleteComment(
            @Parameter(
                    description = "Unique identifier of the comment",
                    example = "50000000-0000-0000-0000-000000000001",
                    required = true
            )
            @PathVariable
            UUID commentId,

            @Parameter(hidden = true)
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        eventCommentService.deleteComment(
                commentId,
                userId
        );
    }
}