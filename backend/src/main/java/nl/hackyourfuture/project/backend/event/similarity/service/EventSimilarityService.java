package nl.hackyourfuture.project.backend.event.similarity.service;

import nl.hackyourfuture.project.backend.event.dto.response.EventSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface EventSimilarityService {
    List<EventSummaryResponse> findSimilarEvents(
            UUID eventId,
            int limit
    );
}
