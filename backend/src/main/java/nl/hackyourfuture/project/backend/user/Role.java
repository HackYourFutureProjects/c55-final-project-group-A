package nl.hackyourfuture.project.backend.user;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {
  USER, ADMIN;

  public static Role fromDbValue(String value) {
    return Role.valueOf(value.toUpperCase());
  }

  @JsonValue
  public String toDbValue() {
    return name().toLowerCase();
  }
}
