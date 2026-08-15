package nl.hackyourfuture.project.backend.auth;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@AllArgsConstructor
public class SessionRepository {

  public static final RowMapper<Session> SESSION_ROW_MAPPER = (rs, _) -> Session.builder()
      .id(rs.getObject("id", UUID.class))
      .userId(rs.getObject("user_id", UUID.class))
      .accessTokenHash(rs.getString("access_token_hash"))
      .accessCreatedAt(rs.getObject("access_created_at", Instant.class))
      .accessExpiresAt(rs.getObject("access_expires_at", Instant.class))
      .build();
  private final JdbcClient jdbcClient;

  public void createSession(Session session) {
    jdbcClient
        .sql("""
            INSERT INTO sessions (user_id, access_token_hash, access_expires_at)
            VALUES (:userId, :accessTokenHash, :accessExpiresAt)
            """
        )
        .param("userId", session.getUserId())
        .param("accessTokenHash", session.getAccessTokenHash())
        .param("accessExpiresAt", session.getAccessExpiresAt())
        .update();
  }

  public Optional<Session> findSessionByAccessTokenHash(String accessTokenHash) {
    return jdbcClient
        .sql("""
            SELECT id, user_id, access_token_hash, access_created_at, access_expires_at
            FROM sessions WHERE access_token_hash = :accessTokenHash
            """)
        .param("accessTokenHash", accessTokenHash)
        .query(SESSION_ROW_MAPPER)
        .optional();
  }

  public void deleteSessionByAccessTokenHash(String accessTokenHash) {
    jdbcClient
        .sql("""
            DELETE FROM sessions WHERE access_token_hash = :accessTokenHash
            """)
        .param("accessTokenHash", accessTokenHash)
        .update();
  }

}
