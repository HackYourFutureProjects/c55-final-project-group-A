package nl.hackyourfuture.project.backend.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Session {
  private UUID id;
  private UUID userId;
  private String accessTokenHash;
  private OffsetDateTime accessCreatedAt;
  private OffsetDateTime accessExpiresAt;
}
