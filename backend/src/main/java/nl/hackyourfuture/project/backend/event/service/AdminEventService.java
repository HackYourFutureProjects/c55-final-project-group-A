package nl.hackyourfuture.project.backend.event.service;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.category.repository.CategoryRepository;
import nl.hackyourfuture.project.backend.event.dto.request.CreateEventRequest;
import nl.hackyourfuture.project.backend.event.dto.response.CreateEventResponse;
import nl.hackyourfuture.project.backend.event.exceptions.EventNotFoundException;
import nl.hackyourfuture.project.backend.event.image.repository.EventImageRepository;
import nl.hackyourfuture.project.backend.event.model.EventDraft;
import nl.hackyourfuture.project.backend.event.repository.EventRepository;
import nl.hackyourfuture.project.backend.user.exceptions.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import nl.hackyourfuture.project.backend.event.image.exceptions.ImageUploadException;
import nl.hackyourfuture.project.backend.event.image.service.ImageService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final EventImageRepository eventImageRepository;
    private final ImageService imageService;

    @Transactional
    public CreateEventResponse createDraft(
            CreateEventRequest request,
            MultipartFile image,
            UUID createdByUserId
    ) {
        validateEventDates(request);
        validateCategoryIds(request.categoryIds());

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

        String imageUrl = null;

        try {
            imageUrl = imageService.upload(eventId, image);
        } catch (ImageUploadException exception) {
            log.warn(
                    "Draft event {} was created, but image upload failed",
                    eventId,
                    exception
            );
        }

        return new CreateEventResponse(eventId, false, imageUrl);
    }

    @Transactional
    public void publish(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new EventNotFoundException("Event not found: " + eventId);
        }

        if (eventRepository.isCancelled(eventId)) {
            throw new BadRequestException(
                    "A cancelled event cannot be published"
            );
        }

        if (!eventImageRepository.existsByEventId(eventId)) {
            throw new BadRequestException(
                    "An event must have an image before it can be published"
            );
        }

        eventRepository.publish(eventId);
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

    private void validateCategoryIds(Set<UUID> categoryIds) {
        Set<UUID> existingCategoryIds = categoryRepository.findAll()
                .stream()
                .map(category -> category.id())
                .collect(Collectors.toSet());

        if (!existingCategoryIds.containsAll(categoryIds)) {
            throw new BadRequestException(
                    "One or more selected categories do not exist"
            );
        }
    }
}
