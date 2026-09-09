package nl.hackyourfuture.project.backend.auth.passwordreset;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.auth.AuthService;
import nl.hackyourfuture.project.backend.auth.SessionRepository;
import nl.hackyourfuture.project.backend.auth.TokenService;
import nl.hackyourfuture.project.backend.auth.dto.AuthResult;
import nl.hackyourfuture.project.backend.auth.passwordreset.exceptions.InvalidResetTokenException;
import nl.hackyourfuture.project.backend.user.User;
import nl.hackyourfuture.project.backend.user.UserRepository;
import nl.hackyourfuture.project.backend.user.exceptions.UserNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

  private static final int MAX_REQUEST_PER_WINDOW = 2;
  private static final int TOKEN_VALIDITY_MINUTES = 15;
  private static final int RATE_LIMIT_WINDOW_HOURS = 24;

  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final UserRepository userRepository;
  private final SessionRepository sessionRepository;
  private final TokenService tokenService;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;
  private final AuthService authService;

  public void requestPasswordReset(String email){
    User user = userRepository.findUserByEmail(email).orElse(null);

    if(user == null || hasExceededRateLimit(user.getId())){
      return;
    }

    String rawToken = tokenService.generateToken();
    String hashedToken = tokenService.hashToken(rawToken);

    PasswordResetToken resetToken = PasswordResetToken.builder()
        .userId(user.getId())
        .tokenHash(hashedToken)
        .expiresAt(OffsetDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES))
        .build();

    passwordResetTokenRepository.createToken(resetToken);

    emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
  }

  public boolean isTokenValid(String rawToken){
    String hashedToken = tokenService.hashToken(rawToken);
    return passwordResetTokenRepository.findValidToken(hashedToken, OffsetDateTime.now()).isPresent();
  }

  @Transactional
  public AuthResult resetPassword(String rawToken, String newPassword){
    String hashedToken = tokenService.hashToken(rawToken);

    PasswordResetToken resetToken = passwordResetTokenRepository
        .findValidToken(hashedToken, OffsetDateTime.now())
        .orElseThrow(() -> new InvalidResetTokenException("Invalid or expired reset link"));

    User user = userRepository.findUserById(resetToken.getUserId())
        .orElseThrow(() -> new UserNotFoundException("User not found"));

    String newHashedPassword = passwordEncoder.encode(newPassword);
    userRepository.updateUserPassword(user.getId(), newHashedPassword);

    passwordResetTokenRepository.markTokenUsed(resetToken.getId());
    sessionRepository.deleteAllSessionsByUserId(user.getId());

    return authService.createSessionAndBuildResult(user);
  }

  private boolean hasExceededRateLimit(UUID userId){
    long recentRequestCount = passwordResetTokenRepository.countRecentRequestsByUserId(
        userId, OffsetDateTime.now().minusHours(RATE_LIMIT_WINDOW_HOURS));
    return recentRequestCount >= MAX_REQUEST_PER_WINDOW;
  }


}
