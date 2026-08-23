package nl.hackyourfuture.project.backend.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.dto.response.EventDetailResponse;
import nl.hackyourfuture.project.backend.event.dto.response.EventPageResponse;
import nl.hackyourfuture.project.backend.event.service.EventService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Operations on events")
public class EventController {

    private final EventService eventService;

    @GetMapping
    @Operation(
            summary = "List events",
            description = """
                    Returns one page of active, published, non-cancelled events
                    ordered by start date.
                    When search is provided, only events with matching titles,
                    descriptions, city names, or category names are returned.
                    Search is case-insensitive.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = """
                    A page of matching events with pagination metadata.
                    The events array is empty when no events match the search.
                    """
    )
    @ApiResponse(
            responseCode = "400",
            description = "Page or size is outside the allowed range"
    )
    public EventPageResponse getEvents(
            @Parameter(
                    description = "Optional text to search for in event titles, descriptions, city names, or category names",
                    example = "music"
            )
            @RequestParam(required = false)
            String search,

            @Parameter(
                    description = "Zero-based page number",
                    example = "0"
            )
            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @Parameter(
                    description = "Number of events per page, between 1 and 100",
                    example = "9"
            )
            @RequestParam(defaultValue = "9")
            @Min(1)
            @Max(100)
            int size
    ) {
        return eventService.getEventPage(search, page, size);
    }

    @GetMapping("/{eventId}")
    @Operation(
            summary = "Get event details",
            description = """
                    Returns the public details of a published event, including
                    its description, category, schedule, location, primary
                    image, attendee count, and calculated status. Cancelled and
                    past published events remain accessible by their ID.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Event details returned successfully"
    )
    @ApiResponse(
            responseCode = "400",
            description = "The supplied event ID is not a valid UUID"
    )
    @ApiResponse(
            responseCode = "404",
            description = "No published event exists with the supplied ID"
    )
    public EventDetailResponse getEventDetail(
            @PathVariable @Parameter(
                    description = "Unique identifier of the event",
                    example = "40000000-0000-0000-0000-000000000001",
                    required = true
            )
            UUID eventId
    ) {
        return eventService.getEventDetail(eventId);
    }
}
