package nl.hackyourfuture.project.backend.event.service;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.dto.request.CreateEventRequest;
import nl.hackyourfuture.project.backend.event.dto.response.CreateEventResponse;
import nl.hackyourfuture.project.backend.event.model.EventDraft;
import nl.hackyourfuture.project.backend.event.repository.EventRepository;
import nl.hackyourfuture.project.backend.user.exceptions.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminEventService {

    private final EventRepository eventRepository;

    @Transactional
    public CreateEventResponse createDraft(
            CreateEventRequest request,
            UUID createdByUserId
    ) {
        validateEventDates(request);

        EventDraft draft = new EventDraft(
                request.title(),
                request.description(),
                request.addressId(),
                request.startAt(),
                request.endAt(),
                request.price(),
                createdByUserId
        );

        UUID eventId = eventRepository.createDraft(draft);

        eventRepository.addCategories(eventId, request.categoryIds());

        return new CreateEventResponse(eventId, false);
    }

    private void validateEventDates(CreateEventRequest request) {
        if (request.startAt() == null
                || request.endAt() == null
                || !request.endAt().isAfter(request.startAt())) {
            throw new BadRequestException(
                    "Event end time must be after the start time"
            );
        }
    }
}