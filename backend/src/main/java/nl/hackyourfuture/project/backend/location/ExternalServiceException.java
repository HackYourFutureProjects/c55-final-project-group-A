package nl.hackyourfuture.project.backend.location;

public class ExternalServiceException extends RuntimeException{
  public ExternalServiceException(String message){
    super(message);
  }
}
