package nl.hackyourfuture.project.backend.feedback.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Topic {
  APP, EVENT;

  public static Topic fromDbValue(String value) {
    return Topic.valueOf(value.toUpperCase());
  }

  @JsonValue
  public String toDbValue() {
    return name().toLowerCase();
  }
}


