package nl.hackyourfuture.project.backend.user.exceptions;

public class UserNotFoundException extends RuntimeException{
public UserNotFoundException(String message){
  super(message);
}
}
