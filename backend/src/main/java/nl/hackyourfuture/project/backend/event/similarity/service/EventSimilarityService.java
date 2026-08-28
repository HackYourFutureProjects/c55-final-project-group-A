package nl.hackyourfuture.project.backend.event.similarity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.hackyourfuture.project.backend.event.dto.response.EventSummaryResponse;
import nl.hackyourfuture.project.backend.event.exceptions.EventNotFoundException;
import nl.hackyourfuture.project.backend.event.similarity.repository.EventSimilarityRepository;
import nl.hackyourfuture.project.backend.user.exceptions.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSimilarityService {

    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 50;

    private final EventSimilarityRepository eventSimilarityRepository;

    public List<EventSummaryResponse> findSimilarEvents(
            UUID eventId,
            int limit
    ) {
        validateLimit(limit);

        log.debug(
                "Finding up to {} similar events for event {}",
                limit,
                eventId
        );

        if (!eventSimilarityRepository.existsPublishedEvent(eventId)) {
            throw new EventNotFoundException(
                    "Event not found: " + eventId
            );
        }

        List<EventSummaryResponse> similarEvents =
                eventSimilarityRepository
                        .findSimilarEvents(eventId, limit)
                        .stream()
                        .map(candidate ->
                                EventSummaryResponse.from(candidate.event())
                        )
                        .toList();

        log.debug(
                "Found {} similar events for event {}",
                similarEvents.size(),
                eventId
        );

        return similarEvents;
    }

    private void validateLimit(int limit) {
        if (limit < MIN_LIMIT || limit > MAX_LIMIT) {
            throw new BadRequestException(
                    "Limit must be between "
                            + MIN_LIMIT
                            + " and "
                            + MAX_LIMIT
            );
        }
    }
}
