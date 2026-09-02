package nl.hackyourfuture.project.backend.user.interactions;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.exceptions.EventNotFoundException;
import nl.hackyourfuture.project.backend.event.repository.EventRegistryRepository;
import nl.hackyourfuture.project.backend.user.UserRepository;
import nl.hackyourfuture.project.backend.user.UserService;
import nl.hackyourfuture.project.backend.user.exceptions.UserNotFoundException;
import nl.hackyourfuture.project.backend.user.interactions.dto.SavedGoingEventCardPageResponse;
import nl.hackyourfuture.project.backend.user.interactions.dto.SavedGoingEventCardResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserEventService {

  private final UserEventRepository userEventRepository;
  private final UserRepository userRepository;
  private final UserService userService;
  private final EventRegistryRepository eventRegistryRepository;

  @Transactional
  public void addEventToSaved(UUID userId, UUID eventId) {
    validateUserAndEvent(userId, eventId);
    eventRegistryRepository.registerEventIfMissing(eventId);
    userEventRepository.addEventToSaved(userId, eventId);
  }

  @Transactional
  public void addEventToGoing(UUID userId, UUID eventId) {
    validateUserAndEvent(userId, eventId);
    eventRegistryRepository.registerEventIfMissing(eventId);
    userEventRepository.addEventToGoing(userId, eventId);
  }

  public void deleteEventFromSaved(UUID userId, UUID eventId) {
    validateUserAndEvent(userId, eventId);
    userEventRepository.deleteEventFromSaved(userId, eventId);
  }

  public void deleteEventFromGoing(UUID userId, UUID eventId) {
    validateUserAndEvent(userId, eventId);
    userEventRepository.deleteEventFromGoing(userId, eventId);
  }

  public SavedGoingEventCardPageResponse getSavedEventsSummary(UUID userId, int page, int size) {
    userService.getUserOrThrow(userId);

    int offset = page * size;

    List<SavedGoingEventCardResponse> events = userEventRepository
        .getSavedEvents(userId, size, offset)
        .stream()
        .map(SavedGoingEventCardResponse::from)
        .toList();

    long totalElements = userEventRepository.countSavedByUser(userId);
    int totalPages = (int) Math.ceil((double) totalElements / size);
    boolean hasNext = page + 1 < totalPages;

    return new SavedGoingEventCardPageResponse(events, page, size, totalElements, totalPages, hasNext);
  }

  public SavedGoingEventCardPageResponse getGoingEventsSummary(UUID userId, int page, int size) {
    userService.getUserOrThrow(userId);

    int offset = page * size;

    List<SavedGoingEventCardResponse> events = userEventRepository
        .getGoingEvents(userId, size, offset)
        .stream()
        .map(SavedGoingEventCardResponse::from)
        .toList();

    long totalElements = userEventRepository.countGoingByUser(userId);
    int totalPages = (int) Math.ceil((double) totalElements / size);
    boolean hasNext = page + 1 < totalPages;

    return new SavedGoingEventCardPageResponse(events, page, size, totalElements, totalPages, hasNext);
  }


  private void validateUserAndEvent(UUID userId, UUID eventId) {
    if (userRepository.findUserById(userId).isEmpty()) {
      throw new UserNotFoundException("User not found");
    }

    if (!userEventRepository.eventExists(eventId)) {
      throw new EventNotFoundException("Event not found");
    }
  }


}
