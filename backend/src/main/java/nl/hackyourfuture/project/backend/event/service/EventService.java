package nl.hackyourfuture.project.backend.event.service;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.dto.response.EventPageResponse;
import nl.hackyourfuture.project.backend.event.dto.response.EventSummaryResponse;
import nl.hackyourfuture.project.backend.event.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
