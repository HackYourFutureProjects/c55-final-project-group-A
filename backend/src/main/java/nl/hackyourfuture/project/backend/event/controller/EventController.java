package nl.hackyourfuture.project.backend.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.dto.EventResponse;
import nl.hackyourfuture.project.backend.event.service.EventService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Operations on events")
public class EventController {

    private final EventService eventService;

    @GetMapping
    @Operation(
            summary = "List all events",
            description = "Returns every event currently stored."
    )
    @ApiResponse(
            responseCode = "200",
            description = "The list of events"
    )
    public List<EventResponse> getEvents() {
        return eventService.getAllEvents();
    }
}

