package nl.hackyourfuture.project.backend.auth.exceptions;

public class InvalidSessionException extends RuntimeException {
  public InvalidSessionException(String message) {
    super(message);
  }
}
