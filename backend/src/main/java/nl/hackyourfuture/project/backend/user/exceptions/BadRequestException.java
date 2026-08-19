package nl.hackyourfuture.project.backend.user.exceptions;

public class BadRequestException extends RuntimeException{
  public BadRequestException(String message){
    super(message);
  }
}