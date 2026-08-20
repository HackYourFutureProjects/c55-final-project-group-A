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
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create an event draft",
            description = """
                    Creates an unpublished event draft. The response contains the
                    event ID needed to upload images before publishing the event.
                    Only admins can use this endpoint.
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
    public CreateEventResponse createDraft(
            @Valid @RequestBody CreateEventRequest request,
            @Parameter(hidden = true) Authentication authentication
    ) {
        UUID createdByUserId = (UUID) authentication.getPrincipal();

        return adminEventService.createDraft(request, createdByUserId);
    }
}