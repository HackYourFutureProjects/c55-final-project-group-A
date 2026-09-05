package nl.hackyourfuture.project.backend.auth.google;

public class GoogleAuthException extends RuntimeException {
  public GoogleAuthException(String message){
    super(message);
  }
}
