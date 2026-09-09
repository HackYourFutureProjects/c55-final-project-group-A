package nl.hackyourfuture.project.backend.auth.passwordreset;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PasswordResetToken {
  private UUID id;
  private UUID userId;
  private String tokenHash;
  private OffsetDateTime createdAt;
  private OffsetDateTime expiresAt;
  private OffsetDateTime usedAt;
}
