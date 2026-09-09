package nl.hackyourfuture.project.backend.auth.passwordreset.exceptions;

public class InvalidResetTokenException extends RuntimeException {
  public InvalidResetTokenException(String message) {
    super(message);
  }
}
