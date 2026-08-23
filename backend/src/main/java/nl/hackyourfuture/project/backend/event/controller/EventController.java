package nl.hackyourfuture.project.backend.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.dto.response.EventDetailResponse;
import nl.hackyourfuture.project.backend.event.dto.response.EventPageResponse;
import nl.hackyourfuture.project.backend.event.model.EventPriceFilter;
import nl.hackyourfuture.project.backend.event.service.EventService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
                    The events array is empty when no events match the search
                    or selected filters.
                    """
    )
    @ApiResponse(
            responseCode = "400",
            description = "One or more query parameters is invalid"
    )
    public EventPageResponse getEvents(
            @Parameter(
                    description = "Optional text to search for in event titles, descriptions, city names, or category names",
                    example = "music"
            )
            @RequestParam(required = false)
            String search,
            @Parameter(
                    description = """
                            Optional category IDs. Repeat the parameter to select multiple
                            categories. Events matching any selected category are returned.
                            """,
                    example = "10000000-0000-0000-0000-000000000001"
            )
            @RequestParam(required = false)
            List<UUID> categoryIds,

            @Parameter(
                    description = """
                            Optional first date of the inclusive event date range,
                            formatted as YYYY-MM-DD. Must be provided with dateTo.
                            """,
                    example = "2026-09-01"
            )
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateFrom,

            @Parameter(
                    description = """
                            Optional last date of the inclusive event date range,
                            formatted as YYYY-MM-DD. Must be provided with dateFrom
                            and must not be before dateFrom.
                            """,
                    example = "2026-09-30"
            )
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateTo,

            @Parameter(
                    description = """
                            Optional latitude of the location filter centre,
                            between -90 and 90. Must be provided with longitude
                            and radiusKm.
                            """,
                    example = "52.3676"
            )
            @RequestParam(required = false)
            @DecimalMin(value = "-90.0", message = "Latitude must be at least -90")
            @DecimalMax(value = "90.0", message = "Latitude must not exceed 90")
            BigDecimal latitude,

            @Parameter(
                    description = """
                            Optional longitude of the location filter centre,
                            between -180 and 180. Must be provided with latitude
                            and radiusKm.
                            """,
                    example = "4.9041"
            )
            @RequestParam(required = false)
            @DecimalMin(value = "-180.0", message = "Longitude must be at least -180")
            @DecimalMax(value = "180.0", message = "Longitude must not exceed 180")
            BigDecimal longitude,

            @Parameter(
                    description = """
                            Optional search radius in kilometres. Must be greater
                            than zero and provided with latitude and longitude.
                            """,
                    example = "10"
            )
            @RequestParam(required = false)
            @DecimalMin(
                    value = "0.0",
                    inclusive = false,
                    message = "Radius must be greater than zero"
            )
            BigDecimal radiusKm,

            @Parameter(
                    description = """
                            Optional price filter. FREE returns events with a zero
                            price; PAID returns events with a price greater than zero.
                            """,
                    example = "FREE"
            )
            @RequestParam(required = false)
            EventPriceFilter price,

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
        return eventService.getEventPage(
                search,
                categoryIds,
                dateFrom,
                dateTo,
                latitude,
                longitude,
                radiusKm,
                price,
                page,
                size
        );
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
