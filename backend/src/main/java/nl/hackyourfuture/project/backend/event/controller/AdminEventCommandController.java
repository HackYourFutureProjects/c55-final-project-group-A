package nl.hackyourfuture.project.backend.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.dto.request.CreateEventRequest;
import nl.hackyourfuture.project.backend.event.dto.response.CreateEventResponse;
import nl.hackyourfuture.project.backend.event.service.AdminEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import nl.hackyourfuture.project.backend.event.dto.request.UpdateEventRequest;
import nl.hackyourfuture.project.backend.event.dto.response.AdminEventDetailResponse;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
@Tag(
        name = "Admin events",
        description = "Admin-only operations for creating and managing events"
)
public class AdminEventCommandController {

    private final AdminEventService adminEventService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create an event",
            description = """
                    Creates an event and uploads its image. Set `publishNow` to
                    `false` to save the event as a draft or to `true` to make it
                    publicly visible immediately.
                    
                    Send a multipart/form-data request with an `event` JSON part
                    and an `image` file part. Images must be JPEG, PNG, or WebP
                    and no larger than 5 MB. Only admins can use this endpoint.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Event created as a draft or published successfully",
            content = @Content(
                    schema = @Schema(implementation = CreateEventResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "The request data is invalid",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required"
    )
    @ApiResponse(
            responseCode = "403",
            description = "Only admins can create events"
    )
    @ApiResponse(
            responseCode = "502",
            description = "The external image service could not upload the image",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public CreateEventResponse createEvent(
            @Parameter(
                    description = "Event data as JSON",
                    required = true
            )
            @Valid @RequestPart("event") CreateEventRequest request,
            @Parameter(
                    description = "Event image: JPEG, PNG, or WebP; maximum 5 MB",
                    required = true,
                    content = @Content(
                            schema = @Schema(type = "string", format = "binary")
                    )
            )
            @RequestPart("image") MultipartFile image,
            @Parameter(
                    description = "Whether to publish the event immediately instead of saving it as a draft",
                    example = "false"
            )
            @RequestParam(defaultValue = "false") boolean publishNow,
            @Parameter(hidden = true) Authentication authentication
    ) {
        UUID createdByUserId = (UUID) authentication.getPrincipal();

        return adminEventService.createEvent(
                request,
                image,
                createdByUserId,
                publishNow
        );
    }

    @PatchMapping("/{eventId}/publish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Publish an event draft",
            description = """
                    Makes an event publicly visible. The event must exist and have
                    an uploaded image. Only admins can use this endpoint.
                    """
    )
    @ApiResponse(
            responseCode = "204",
            description = "Event published successfully"
    )
    @ApiResponse(
            responseCode = "400",
            description = "The event does not have an image",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required"
    )
    @ApiResponse(
            responseCode = "403",
            description = "Only admins can publish event drafts"
    )
    @ApiResponse(
            responseCode = "404",
            description = "The event does not exist",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public void publish(
            @Parameter(
                    description = "ID of the event to publish",
                    required = true
            )
            @PathVariable UUID eventId
    ) {
        adminEventService.publish(eventId);
    }

    @PatchMapping(
            value = "/{eventId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Update an event",
            description = """
                    Updates selected fields of an existing draft or published event.
                    Omitted fields remain unchanged. A replacement image is optional.
                    Cancelled events cannot be edited.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Event updated successfully",
            content = @Content(
                    schema = @Schema(
                            implementation = AdminEventDetailResponse.class
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = """
                    The update is empty, contains invalid data, has invalid dates,
                    or targets a cancelled event
                    """,
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required"
    )
    @ApiResponse(
            responseCode = "403",
            description = "Only administrators can update events"
    )
    @ApiResponse(
            responseCode = "404",
            description = "The event does not exist",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "502",
            description = "The external image service could not upload the replacement image",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public AdminEventDetailResponse updateEvent(
            @Parameter(
                    description = "Unique identifier of the event",
                    example = "40000000-0000-0000-0000-000000000001",
                    required = true
            )
            @PathVariable UUID eventId,

            @Parameter(
                    description = "Event fields to update as JSON",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    implementation = UpdateEventRequest.class
                            )
                    )
            )
            @Valid
            @RequestPart("event")
            UpdateEventRequest request,

            @Parameter(
                    description = """
                            Optional replacement image. Must be JPEG, PNG, or WebP
                            and no larger than 5 MB.
                            """,
                    content = @Content(
                            schema = @Schema(
                                    type = "string",
                                    format = "binary"
                            )
                    )
            )
            @RequestPart(
                    value = "image",
                    required = false
            )
            MultipartFile image
    ) {
        return adminEventService.updateEvent(
                eventId,
                request,
                image
        );
    }

    @PatchMapping("/{eventId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Cancel a published event",
            description = """
                    Cancels an upcoming or ongoing published event. The event remains
                    stored so users who saved it or marked themselves as going can
                    see that it was cancelled. Draft and past events cannot be
                    cancelled. Repeated cancellation requests are accepted.
                    """
    )
    @ApiResponse(
            responseCode = "204",
            description = "Event cancelled successfully or was already cancelled"
    )
    @ApiResponse(
            responseCode = "400",
            description = """
                    The event ID is invalid, the event is a draft, or the event
                    has already ended
                    """,
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required"
    )
    @ApiResponse(
            responseCode = "403",
            description = "Only administrators can cancel events"
    )
    @ApiResponse(
            responseCode = "404",
            description = "The event does not exist",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public void cancel(
            @Parameter(
                    description = "Unique identifier of the event to cancel",
                    example = "40000000-0000-0000-0000-000000000001",
                    required = true
            )
            @PathVariable UUID eventId
    ) {
        adminEventService.cancel(eventId);
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete an event draft",
            description = """
                    Permanently deletes an unpublished event draft and its related
                    database records. Published events cannot be deleted and must
                    be cancelled instead.
                    """
    )
    @ApiResponse(
            responseCode = "204",
            description = "Event draft deleted successfully"
    )
    @ApiResponse(
            responseCode = "400",
            description = """
                    The event ID is invalid or the event has already been published
                    and cannot be permanently deleted
                    """,
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required"
    )
    @ApiResponse(
            responseCode = "403",
            description = "Only administrators can delete event drafts"
    )
    @ApiResponse(
            responseCode = "404",
            description = "The event does not exist",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public void deleteDraft(
            @Parameter(
                    description = "Unique identifier of the draft to delete",
                    example = "40000000-0000-0000-0000-000000000001",
                    required = true
            )
            @PathVariable UUID eventId
    ) {
        adminEventService.deleteDraft(eventId);
    }
}
