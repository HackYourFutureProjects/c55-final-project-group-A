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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(
        name = "Admin events",
        description = "Admin-only operations for creating and managing events"
)
public class AdminEventController {

    private final AdminEventService adminEventService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create an event draft",
            description = """
                    Creates an unpublished event draft and uploads its image.
                    Send a multipart/form-data request with an `event` JSON part
                    and an `image` file part. Images must be JPEG, PNG, or WebP
                    and no larger than 5 MB. Only admins can use this endpoint.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Event draft created successfully",
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
            description = "Only admins can create event drafts"
    )
    @ApiResponse(
            responseCode = "502",
            description = "The external image service could not upload the image",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public CreateEventResponse createDraft(
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
            @Parameter(hidden = true) Authentication authentication
    ) {
        UUID createdByUserId = (UUID) authentication.getPrincipal();

        return adminEventService.createDraft(request, image, createdByUserId);
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
}
