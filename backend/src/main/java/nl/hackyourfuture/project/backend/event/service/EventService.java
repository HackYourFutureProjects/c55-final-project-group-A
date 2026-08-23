package nl.hackyourfuture.project.backend.event.service;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.dto.response.EventDetailResponse;
import nl.hackyourfuture.project.backend.event.dto.response.EventPageResponse;
import nl.hackyourfuture.project.backend.event.dto.response.EventSummaryResponse;
import nl.hackyourfuture.project.backend.event.exceptions.EventNotFoundException;
import nl.hackyourfuture.project.backend.event.model.EventDetail;
import nl.hackyourfuture.project.backend.event.model.EventQueryCriteria;
import nl.hackyourfuture.project.backend.event.model.EventStatus;
import nl.hackyourfuture.project.backend.event.repository.EventRepository;
import org.springframework.stereotype.Service;
import nl.hackyourfuture.project.backend.user.exceptions.BadRequestException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    public EventPageResponse getEventPage(
            String search,
            List<UUID> categoryIds,
            LocalDate dateFrom,
            LocalDate dateTo,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal radiusKm,
            int page,
            int size
    ) {
        EventQueryCriteria criteria = new EventQueryCriteria(
                search,
                categoryIds,
                dateFrom,
                dateTo,
                latitude,
                longitude,
                radiusKm,
                null,       // price
                List.of()   // timesOfDay
        );
        if (criteria.hasAnyDateFilter()
                && !criteria.hasCompleteDateFilter()) {
            throw new BadRequestException(
                    "dateFrom and dateTo must be provided together"
            );
        }

        if (criteria.hasCompleteDateFilter()
                && criteria.dateFrom().isAfter(criteria.dateTo())) {
            throw new BadRequestException(
                    "dateFrom must not be after dateTo"
            );


        }
        if (criteria.hasAnyLocationFilter()
                && !criteria.hasCompleteLocationFilter()) {
            throw new BadRequestException(
                    "latitude, longitude, and radiusKm must be provided together"
            );
        }

        int offset = page * size;

        List<EventSummaryResponse> events = eventRepository
                .findEventSummaries(criteria, size, offset)
                .stream()
                .map(EventSummaryResponse::from)
                .toList();

        long totalElements = eventRepository.countEvents(criteria);

        int totalPages = (int) Math.ceil(
                (double) totalElements / size
        );

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
