package nl.hackyourfuture.project.backend.user;

public enum Role {
  USER, ADMIN;

  public static Role fromDbValue(String value) {
    return Role.valueOf(value.toUpperCase());
  }

  public String toDbValue() {
    return name().toLowerCase();
  }
}
