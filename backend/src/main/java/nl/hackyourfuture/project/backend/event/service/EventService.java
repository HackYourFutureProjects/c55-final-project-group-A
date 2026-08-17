package nl.hackyourfuture.project.backend.event.service;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.dto.response.EventDetailResponse;
import nl.hackyourfuture.project.backend.event.dto.response.EventPageResponse;
import nl.hackyourfuture.project.backend.event.dto.response.EventSummaryResponse;
import nl.hackyourfuture.project.backend.event.exceptions.EventNotFoundException;
import nl.hackyourfuture.project.backend.event.model.EventDetail;
import nl.hackyourfuture.project.backend.event.model.EventStatus;
import nl.hackyourfuture.project.backend.event.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    public EventPageResponse getEventPage(
            String search,
            int page,
            int size
    ) {
        String normalizedSearch =
                search == null || search.isBlank() ? null : search.trim();

        int offset = page * size;

        List<EventSummaryResponse> events = eventRepository
                .findEventSummaries(normalizedSearch, size, offset)
                .stream()
                .map(EventSummaryResponse::from)
                .toList();

        long totalElements = eventRepository.countEvents(normalizedSearch);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = page + 1 < totalPages;

        return new EventPageResponse(
                events,
                page,
                size,
                totalElements,
                totalPages,
                hasNext
        );
    }

    private EventStatus determineStatus(EventDetail event) {
        if (event.cancelled()) {
            return EventStatus.CANCELLED;
        }
        OffsetDateTime now = OffsetDateTime.now(java.time.ZoneOffset.UTC);

        if (now.isBefore(event.startAt())) {
            return EventStatus.UPCOMING;
        }

        if (now.isBefore(event.endAt())) {
            return EventStatus.ONGOING;
        }

        return EventStatus.PAST;
    }

    public EventDetailResponse getEventDetail(UUID eventId) {
        EventDetail event = eventRepository
                .findEventDetailById(eventId)
                .orElseThrow(() ->
                        new EventNotFoundException("Event not found: " + eventId)
                );

        EventStatus status = determineStatus(event);
        return EventDetailResponse.from(event, status);
    }
}
