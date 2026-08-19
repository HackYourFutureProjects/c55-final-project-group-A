package nl.hackyourfuture.project.backend.user;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.auth.exceptions.EmailAlreadyExistsException;
import nl.hackyourfuture.project.backend.user.dto.PatchUserRequest;
import nl.hackyourfuture.project.backend.user.dto.UserResponse;
import nl.hackyourfuture.project.backend.user.exceptions.BadRequestException;
import nl.hackyourfuture.project.backend.user.exceptions.UserNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;

  public UserResponse getCurrentUser(UUID id) {
    User user = getUserOrThrow(id);
    return UserResponse.from(user);
  }

  public UserResponse updateUser(UUID id, PatchUserRequest request) {
      getUserOrThrow(id);

    boolean emailBlank = request.email() == null || request.email().isBlank();
    boolean nameBlank = request.name() == null || request.name().isBlank();
    boolean locationBlank = request.location() == null || request.location().isBlank();

    if (emailBlank && nameBlank && locationBlank) {
      throw new BadRequestException("At least one field must be provided");
    }

    if (!emailBlank) {
      userRepository.findUserByEmail(request.email())
          .filter(existingUser -> !existingUser.getId().equals(id))
          .ifPresent(existingUser -> {
            throw new EmailAlreadyExistsException("Email must be unique");
          });
    }

    var updatedUser = User.builder()
        .id(id)
        .email(request.email())
        .name(request.name())
        .location(request.location())
        .build();
    var updated = userRepository.updateUser(updatedUser);
    return UserResponse.from(updated);
  }

  public void deleteUser(UUID id) {
      getUserOrThrow(id);
    userRepository.deleteUserById(id);
  }

  private User getUserOrThrow(UUID id){
      return userRepository.findUserById(id)
          .orElseThrow(() -> new UserNotFoundException("User not found"));
  }
}
