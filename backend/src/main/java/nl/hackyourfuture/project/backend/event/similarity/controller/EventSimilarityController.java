package nl.hackyourfuture.project.backend.event.similarity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.dto.response.EventSummaryResponse;
import nl.hackyourfuture.project.backend.event.similarity.service.EventSimilarityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(
        name = "Event similarity",
        description = "Operations for discovering similar events"
)
public class EventSimilarityController {

    private static final int SIMILAR_EVENTS_LIMIT = 5;

    private final EventSimilarityService eventSimilarityService;

    @GetMapping("/{eventId}/similar")
    @Operation(
            summary = "Get similar events",
            description = """
                    Returns up to five published, active, and non-cancelled
                    events ranked by their similarity to the selected event.
                    
                    Similarity is calculated using categories, city,
                    time of day, weekday, and price type. Popularity is
                    used only as a tie-breaker.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = """
                    Similar events returned successfully. The response is
                    an empty array when no eligible candidates exist.
                    """
    )
    @ApiResponse(
            responseCode = "400",
            description = "The supplied event ID is not a valid UUID"
    )
    @ApiResponse(
            responseCode = "404",
            description = """
                    No published source event exists with the supplied ID
                    """
    )
    public List<EventSummaryResponse> getSimilarEvents(
            @PathVariable
            @Parameter(
                    description = "Unique identifier of the source event",
                    example = "40000000-0000-0000-0000-000000000001",
                    required = true
            )
            UUID eventId
    ) {
        return eventSimilarityService.findSimilarEvents(
                eventId,
                SIMILAR_EVENTS_LIMIT
        );
    }
}