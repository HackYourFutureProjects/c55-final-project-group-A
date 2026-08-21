package nl.hackyourfuture.project.backend.event.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import nl.hackyourfuture.project.backend.event.dto.response.AdminEventPageResponse;
import nl.hackyourfuture.project.backend.event.service.AdminEventService;
import nl.hackyourfuture.project.backend.event.dto.response.AdminEventDetailResponse;

@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
@Tag(
        name = "Admin events",
        description = "Admin-only operations for viewing and managing events"
)
public class AdminEventQueryController {

    private final AdminEventService adminEventService;

    @GetMapping
    @Operation(
            summary = "List events for administrators",
            description = """
                    Returns a paginated list of all events, including unpublished
                    drafts, published events, cancelled events, and past events.
                    Only administrators can access this endpoint.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Admin event page returned successfully",
            content = @Content(
                    schema = @Schema(
                            implementation = AdminEventPageResponse.class
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Page or size is outside the allowed range",
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
            description = "Only administrators can access this endpoint"
    )
    public AdminEventPageResponse getAdminEvents(
            @Parameter(
                    description = "Zero-based page number",
                    example = "0"
            )
            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @Parameter(
                    description = "Number of events per page, between 1 and 100",
                    example = "10"
            )
            @RequestParam(defaultValue = "10")
            @Min(1)
            @Max(100)
            int size
    ) {
        return adminEventService.getAdminEventPage(page, size);
    }

    @GetMapping("/{eventId}")
    @Operation(
            summary = "Get an event for administration",
            description = """
                    Returns complete event information for an administrator.
                    The event can be a draft, published, cancelled, or past event.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Event details returned successfully",
            content = @Content(
                    schema = @Schema(
                            implementation = AdminEventDetailResponse.class
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Event ID is not a valid UUID",
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
            description = "Only administrators can access this endpoint"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Event was not found",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public AdminEventDetailResponse getAdminEventDetail(
            @Parameter(
                    description = "Unique identifier of the event",
                    example = "40000000-0000-0000-0000-000000000001",
                    required = true
            )
            @PathVariable UUID eventId
    ) {
        return adminEventService.getAdminEventDetail(eventId);
    }
}