package nl.hackyourfuture.project.backend.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.hackyourfuture.project.backend.notification.model.OutboxPayload;
import nl.hackyourfuture.project.backend.notification.service.NotificationOutboxService;
import nl.hackyourfuture.project.backend.notification.service.NotificationReminderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.hackyourfuture.project.backend.event.category.repository.CategoryRepository;
import nl.hackyourfuture.project.backend.event.category.repository.EventCategoryRepository;
import nl.hackyourfuture.project.backend.event.dto.request.CreateEventRequest;
import nl.hackyourfuture.project.backend.event.dto.request.UpdateEventRequest;
import nl.hackyourfuture.project.backend.event.dto.response.AdminEventDetailResponse;
import nl.hackyourfuture.project.backend.event.dto.response.AdminEventPageResponse;
import nl.hackyourfuture.project.backend.event.dto.response.AdminEventSummaryResponse;
import nl.hackyourfuture.project.backend.event.dto.response.CreateEventResponse;
import nl.hackyourfuture.project.backend.event.exceptions.EventNotFoundException;
import nl.hackyourfuture.project.backend.event.image.repository.EventImageRepository;
import nl.hackyourfuture.project.backend.event.image.service.ImageService;
import nl.hackyourfuture.project.backend.event.model.AdminEventDetail;
import nl.hackyourfuture.project.backend.event.model.EventUpdate;
import nl.hackyourfuture.project.backend.event.model.NewEvent;
import nl.hackyourfuture.project.backend.event.repository.AddressRepository;
import nl.hackyourfuture.project.backend.event.repository.AdminEventRepository;
import nl.hackyourfuture.project.backend.event.repository.EventRepository;
import nl.hackyourfuture.project.backend.user.exceptions.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final EventCategoryRepository eventCategoryRepository;
    private final EventImageRepository eventImageRepository;
    private final ImageService imageService;
    private final AddressRepository addressRepository;
    private final AdminEventRepository adminEventRepository;
    private final NotificationOutboxService notificationOutboxService;
    private final NotificationReminderService notificationReminderService;

    @Transactional
    public CreateEventResponse createEvent(
            CreateEventRequest request,
            MultipartFile image,
            UUID createdByUserId,
            boolean publishNow
    ) {
        log.debug(
                "Creating event for user {}, publishNow={}",
                createdByUserId,
                publishNow
        );

        validateEventDates(
                request.startAt(),
                request.endAt()
        );
        if (publishNow) {
            validateEventCanBePublished(request.endAt());
        }
        validateCategoryIds(request.categoryIds());
        UUID addressId = addressRepository.create(request.address());

        NewEvent newEvent = new NewEvent(
                request.title(),
                request.description(),
                addressId,
                request.startAt(),
                request.endAt(),
                request.price(),
                createdByUserId
        );

        UUID eventId = eventRepository.createEvent(newEvent);

        eventCategoryRepository.addCategories(eventId, request.categoryIds());

        String imageUrl = imageService.upload(eventId, image);

        if (publishNow) {
            boolean published = eventRepository.publish(eventId);
            if (!published) {
                throw new BadRequestException(
                        "The event could not be published because its status or "
                                + "end time changed"
                );
            }
        }

        log.debug(
                "Created event {} for user {}, published={}",
                eventId,
                createdByUserId,
                publishNow
        );

        return new CreateEventResponse(eventId, publishNow, imageUrl);
    }

    @Transactional
    public void publish(UUID eventId) {
        log.debug("Publishing event {}", eventId);

        AdminEventDetail event = findAdminEvent(eventId);

        if (event.cancelled()) {
            throw new BadRequestException(
                    "A cancelled event cannot be published"
            );
        }

        if (event.published()) {
            log.debug("Event {} is already published", eventId);
            return;
        }

        validateEventCanBePublished(event.endAt());

        if (!eventImageRepository.existsByEventId(eventId)) {
            throw new BadRequestException(
                    "An event must have an image before it can be published"
            );
        }

        boolean published = eventRepository.publish(eventId);
        if (!published) {
            throw new BadRequestException(
                    "The event could not be published because its status or "
                            + "end time changed. Refresh the event and try again."
            );
        }

        log.debug("Published event {}", eventId);
    }

    private void validateEventCanBePublished(OffsetDateTime endAt) {
        if (!endAt.isAfter(OffsetDateTime.now())) {
            throw new BadRequestException(
                    "A past event cannot be published"
            );
        }
    }

    private void validateEventDates(
            OffsetDateTime startAt,
            OffsetDateTime endAt
    ) {
        if (startAt == null
                || endAt == null
                || !endAt.isAfter(startAt)) {
            throw new BadRequestException(
                    "Event end time must be after the start time"
            );
        }
    }

    private void validateCategoryIds(Set<UUID> categoryIds) {
        if (!categoryRepository.existsAllByIds(categoryIds)) {
            throw new BadRequestException(
                    "One or more selected categories do not exist"
            );
        }
    }

    @Transactional(readOnly = true)
    public AdminEventPageResponse getAdminEventPage(
            int page,
            int size
    ) {
        int offset = page * size;

        List<AdminEventSummaryResponse> events =
                adminEventRepository
                        .findAdminEventSummaries(size, offset)
                        .stream()
                        .map(AdminEventSummaryResponse::from)
                        .toList();

        long totalElements = adminEventRepository.countAdminEvents();

        int totalPages = (int) Math.ceil(
                (double) totalElements / size
        );

        boolean hasNext = page + 1 < totalPages;

        return new AdminEventPageResponse(
                events,
                page,
                size,
                totalElements,
                totalPages,
                hasNext
        );
    }

    @Transactional(readOnly = true)
    public AdminEventDetailResponse getAdminEventDetail(UUID eventId) {
        return AdminEventDetailResponse.from(
                findAdminEvent(eventId)
        );
    }

    private AdminEventDetail findAdminEvent(UUID eventId) {
        return adminEventRepository
                .findEventDetailById(eventId)
                .orElseThrow(() ->
                        new EventNotFoundException(
                                "Event not found: " + eventId
                        )
                );
    }

    private EventUpdate buildEventUpdate(
            AdminEventDetail existingEvent,
            UpdateEventRequest request
    ) {
        String updatedTitle = request.title() != null
                ? request.title().trim()
                : existingEvent.title();

        String updatedDescription = existingEvent.description();

        if (request.description() != null) {
            updatedDescription = request.description().isBlank()
                    ? null
                    : request.description().trim();
        }

        OffsetDateTime updatedStartAt = request.startAt() != null
                ? request.startAt()
                : existingEvent.startAt();

        OffsetDateTime updatedEndAt = request.endAt() != null
                ? request.endAt()
                : existingEvent.endAt();

        BigDecimal updatedPrice = request.price() != null
                ? request.price()
                : existingEvent.price();

        validateEventDates(updatedStartAt, updatedEndAt);

        return new EventUpdate(
                existingEvent.id(),
                updatedTitle,
                updatedDescription,
                updatedStartAt,
                updatedEndAt,
                updatedPrice
        );
    }

    @Transactional
    public AdminEventDetailResponse updateEvent(
            UUID eventId,
            UpdateEventRequest request,
            MultipartFile image
    ) {
        log.debug("Updating event {}", eventId);

        AdminEventDetail existingEvent = findAdminEvent(eventId);

        if (existingEvent.cancelled()) {
            throw new BadRequestException(
                    "A cancelled event cannot be edited"
            );
        }

        boolean noEventFieldsProvided =
                request.title() == null
                        && request.description() == null
                        && request.categoryIds() == null
                        && request.address() == null
                        && request.startAt() == null
                        && request.endAt() == null
                        && request.price() == null;

        boolean noImageProvided = image == null || image.isEmpty();

        if (noEventFieldsProvided && noImageProvided) {
            throw new BadRequestException(
                    "Please provide at least one field or image to update"
            );
        }

        if (request.categoryIds() != null) {
            validateCategoryIds(request.categoryIds());
        }

        EventUpdate eventUpdate =
                buildEventUpdate(existingEvent, request);

        boolean startTimeChanged =
                request.startAt() != null
                        && !request.startAt().equals(existingEvent.startAt());

        if (!adminEventRepository.updateEvent(eventUpdate)) {
            throw new EventNotFoundException(
                    "Event not found: " + eventId
            );
        }

        if (request.address() != null
                && !addressRepository.updateForEvent(
                eventId,
                request.address()
        )) {
            throw new EventNotFoundException(
                    "Event address not found: " + eventId
            );
        }

        if (request.categoryIds() != null) {
            eventCategoryRepository.replaceCategories(
                    eventId,
                    request.categoryIds()
            );
        }

        if (!noImageProvided) {
            imageService.upload(eventId, image);
        }

        AdminEventDetail updatedEvent = findAdminEvent(eventId);

        if (startTimeChanged) {
            int deletedReminders = notificationReminderService.deleteRemindersForEvent(eventId);
            log.info(
                    "Deleted {} EVENT_REMINDER notifications for event {} after start time change",
                    deletedReminders,
                    eventId
            );
        }

        if (existingEvent.published()) {
            log.info(
                    "Updated published event {}, enqueuing EVENT_UPDATED notification",
                    eventId
            );
            notificationOutboxService.enqueueEventUpdated(
                    eventId,
                    new OutboxPayload(
                            updatedEvent.title(),
                            "/events/" + eventId
                    )
            );
        }

        log.debug("Updated event {}", eventId);

        return AdminEventDetailResponse.from(updatedEvent);

    }

    @Transactional
    public void unpublish(UUID eventId) {
        log.debug("Unpublishing event {}", eventId);

        AdminEventDetail event = findAdminEvent(eventId);

        if (!event.published()) {
            throw new BadRequestException(
                    "The event is already a draft"
            );
        }

        if (event.cancelled()) {
            throw new BadRequestException(
                    "Cancelled events cannot be unpublished"
            );
        }

        if (!adminEventRepository.unpublishEvent(eventId)) {
            throw new EventNotFoundException(
                    "Event not found: " + eventId
            );
        }

        log.debug("Unpublished event {}", eventId);
    }

    @Transactional
    public void cancel(UUID eventId) {
        log.debug("Cancelling event {}", eventId);

        AdminEventDetail event = findAdminEvent(eventId);

        if (!event.published()) {
            throw new BadRequestException(
                    "A draft event cannot be cancelled; delete it instead"
            );
        }

        if (event.cancelled()) {
            log.warn(
                    "Cancel no-op: event {} is already cancelled",
                    eventId
            );
            return;
        }

        if (!event.endAt().isAfter(OffsetDateTime.now())) {
            throw new BadRequestException(
                    "A past event cannot be cancelled"
            );
        }

        if (!adminEventRepository.cancelEvent(eventId)) {
            log.warn(
                    "Cancel race/failure: event {} could not be cancelled",
                    eventId
            );
            throw new BadRequestException(
                    "The event could not be cancelled because its status "
                            + "changed. Refresh the event and try again."
            );
        }

        log.info(
                "Cancelled event {}, enqueuing EVENT_CANCELLED notification",
                eventId
        );
        notificationOutboxService.enqueueEventCancelled(
                eventId,
                new OutboxPayload(
                        event.title(),
                        "/events/" + eventId
                )
        );

        log.debug("Cancelled event {}", eventId);
    }

    @Transactional
    public void deleteDraft(UUID eventId) {
        log.debug("Deleting draft event {}", eventId);

        AdminEventDetail event = findAdminEvent(eventId);

        if (event.published()) {
            throw new BadRequestException(
                    "A published event cannot be deleted; cancel it instead"
            );
        }

        UUID addressId = adminEventRepository
                .deleteEventById(eventId)
                .orElseThrow(() ->
                        new EventNotFoundException(
                                "Event not found: " + eventId
                        )
                );

        addressRepository.deleteIfUnused(addressId);

        log.debug("Deleted draft event {}", eventId);
    }
}
