package nl.hackyourfuture.project.backend.auth.passwordreset.exceptions;

public class EmailSendingException extends RuntimeException {
  public EmailSendingException(String message) {
    super(message);
  }
}
