package nl.hackyourfuture.project.backend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepository {
  public static final RowMapper<User> USER_ROW_MAPPER = (rs, _) -> User.builder()
      .id(rs.getObject("id", UUID.class))
      .email(rs.getString("email"))
      .role(Role.fromDbValue(rs.getString("role")))
      .name(rs.getString("name"))
      .passwordHash(rs.getString("password_hash"))
      .createdAt(rs.getObject("created_at", OffsetDateTime.class))
      .location(rs.getString("location"))
      .build();
  private final JdbcClient jdbcClient;

  public User createUser(User user) {
    return jdbcClient
        .sql("""
            INSERT INTO users (email, name, password_hash)
            VALUES (:email, :name, :passwordHash)
            RETURNING id, email, role, name, password_hash, created_at, location
            """)
        .param("email", user.getEmail())
        .param("name", user.getName())
        .param("passwordHash", user.getPasswordHash())
        .query(USER_ROW_MAPPER)
        .single();
  }

  public Optional<User> findUserById(UUID id) {
    return jdbcClient
        .sql("""
            SELECT id, email, role, name, password_hash, created_at, location
            FROM users WHERE id = :id
            """)
        .param("id", id)
        .query(USER_ROW_MAPPER)
        .optional();
  }

  public Optional<User> findUserByEmail(String email) {
    return jdbcClient
        .sql("""
            SELECT id, email, role, name, password_hash, created_at, location
            FROM users WHERE email = :email
            """)
        .param("email", email)
        .query(USER_ROW_MAPPER)
        .optional();
  }

  public User updateUser(User user) {
    return jdbcClient.sql("""
            UPDATE users
            SET email = COALESCE(:email, email),
            name = COALESCE(:name, name),
            location = COALESCE(:location, location)
            WHERE id = :id
            RETURNING id, email, role, name, password_hash, created_at, location
            """)
        .param("id", user.getId())
        .param("email", user.getEmail())
        .param("name", user.getName())
        .param("location", user.getLocation())
        .query(USER_ROW_MAPPER)
        .single();

  }

  public void deleteUserById(UUID id) {
    jdbcClient
        .sql("DELETE FROM users WHERE id = :id")
        .param("id", id)
        .update();
  }
}
