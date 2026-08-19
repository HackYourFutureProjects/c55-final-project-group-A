package nl.hackyourfuture.project.backend.user.interactions;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.exceptions.EventNotFoundException;
import nl.hackyourfuture.project.backend.user.UserRepository;
import nl.hackyourfuture.project.backend.user.exceptions.UserNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserEventService {

  private final UserEventRepository userEventRepository;
  private final UserRepository userRepository;

  public void addEventToSaved(UUID userId, UUID eventId){
    validateUserAndEvent(userId, eventId);
    userEventRepository.addEventToSaved(userId, eventId);
  }

  public void addEventToGoing(UUID userId, UUID eventId){
    validateUserAndEvent(userId, eventId);
    userEventRepository.addEventToGoing(userId, eventId);
  }

  public void deleteEventFromSaved(UUID userId, UUID eventId){
    validateUserAndEvent(userId, eventId);
    userEventRepository.deleteEventFromSaved(userId, eventId);
  }

  public void deleteEventFromGoing(UUID userId, UUID eventId){
    validateUserAndEvent(userId, eventId);
    userEventRepository.deleteEventFromGoing(userId, eventId);
  }

  private void validateUserAndEvent(UUID userId, UUID eventId){
    if(userRepository.findUserById(userId).isEmpty()){
      throw new UserNotFoundException("User not found");
    }

    if(!userEventRepository.eventExists(eventId)){
      throw new EventNotFoundException("Event not found");
    }
  }


}
