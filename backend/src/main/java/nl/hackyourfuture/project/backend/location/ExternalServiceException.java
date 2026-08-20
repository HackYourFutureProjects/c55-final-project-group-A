package nl.hackyourfuture.project.backend.location.exceptions;

public class ExternalServiceException extends RuntimeException{
  public ExternalServiceException(String message){
    super(message);
  }
}
