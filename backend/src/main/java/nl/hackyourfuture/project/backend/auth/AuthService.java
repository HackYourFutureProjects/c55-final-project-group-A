package nl.hackyourfuture.project.backend.auth;

import lombok.AllArgsConstructor;
import nl.hackyourfuture.project.backend.auth.dto.AuthResponse;
import nl.hackyourfuture.project.backend.auth.dto.AuthResult;
import nl.hackyourfuture.project.backend.auth.dto.LoginRequest;
import nl.hackyourfuture.project.backend.auth.dto.RegisterRequest;
import nl.hackyourfuture.project.backend.auth.exceptions.EmailAlreadyExistsException;
import nl.hackyourfuture.project.backend.auth.exceptions.InvalidCredentialsException;
import nl.hackyourfuture.project.backend.user.User;
import nl.hackyourfuture.project.backend.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@AllArgsConstructor
public class AuthService {

  private SessionRepository sessionRepository;
  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private TokenService tokenService;

  public AuthResult register(RegisterRequest request){
    if(userRepository.findUserByEmail(request.email()).isPresent()){
      throw new EmailAlreadyExistsException("This email already exist");
    }

    String encodedPassword = passwordEncoder.encode(request.password());

    User newUser = User.builder()
        .name(request.name())
        .email(request.email())
        .passwordHash(encodedPassword)
        .build();

    User created = userRepository.createUser(newUser);

    return createSessionAndBuildResult(created);
  }

  public AuthResult login(LoginRequest request){
    User user = userRepository.findUserByEmail(request.email())
        .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

    if(!passwordEncoder.matches(request.password(), user.getPasswordHash())){
      throw new InvalidCredentialsException("Invalid email or password");
    }

    return createSessionAndBuildResult(user);
  }

  public void logout(String rawAccessToken){
    String hashedAccessToken = tokenService.hashToken(rawAccessToken);
    sessionRepository.deleteSessionByAccessTokenHash(hashedAccessToken);
  }

  private AuthResult createSessionAndBuildResult(User user){
    String rawAccessToken = tokenService.generateToken();
    String hashedAccessToken = tokenService.hashToken(rawAccessToken);

    Session newSession = Session.builder()
        .userId(user.getId())
        .accessTokenHash(hashedAccessToken)
        .accessExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(2))
        .build();

    sessionRepository.createSession(newSession);

    return new AuthResult(AuthResponse.from(user), rawAccessToken);
  }


}
