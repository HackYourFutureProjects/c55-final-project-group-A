package nl.hackyourfuture.project.backend.auth.passwordreset;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PasswordResetTokenRepository {

  public static final RowMapper<PasswordResetToken> PASSWORD_RESET_TOKEN_ROW_MAPPER = (rs, _) -> PasswordResetToken.builder()
      .id(rs.getObject("id", UUID.class))
      .userId(rs.getObject("user_id", UUID.class))
      .tokenHash(rs.getString("token_hash"))
      .createdAt(rs.getObject("created_at", OffsetDateTime.class))
      .expiresAt(rs.getObject("expires_at", OffsetDateTime.class))
      .usedAt(rs.getObject("used_at", OffsetDateTime.class))
      .build();
  private final JdbcClient jdbcClient;

  public void createToken(PasswordResetToken token){
    jdbcClient
        .sql("""
            INSERT INTO password_reset_tokens (user_id, token_hash, expires_at)
            VALUES (:userId, :tokenHash, :expiresAt)
            """)
        .param("userId", token.getUserId())
        .param("tokenHash", token.getTokenHash())
        .param("expiresAt", token.getExpiresAt())
        .update();
  }

  public Optional<PasswordResetToken> findValidToken(String tokenHash, OffsetDateTime now){
    return jdbcClient
        .sql("""
                SELECT id, user_id, token_hash, created_at, expires_at, used_at
                FROM password_reset_tokens
                WHERE token_hash = :tokenHash
                AND expires_at > :now
                AND used_at IS NULL
                """
        )
        .param("tokenHash", tokenHash)
        .param("now", now)
        .query(PASSWORD_RESET_TOKEN_ROW_MAPPER)
        .optional();
  }

  public void markTokenUsed(UUID tokenId){
    jdbcClient
        .sql("""
            UPDATE password_reset_tokens
            SET used_at = now()
            WHERE id = :tokenId
            """)
        .param("tokenId", tokenId)
        .update();
  }

  public long countRecentRequestsByUserId(UUID userId, OffsetDateTime since){
    return jdbcClient
        .sql("""
            SELECT COUNT(*) FROM password_reset_tokens
            WHERE user_id = :userId AND created_at > :since
            """)
        .param("userId", userId)
        .param("since", since)
        .query(Long.class)
        .single();
  }
}
