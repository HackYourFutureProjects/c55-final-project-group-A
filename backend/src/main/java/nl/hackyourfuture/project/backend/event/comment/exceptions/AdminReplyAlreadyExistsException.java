package nl.hackyourfuture.project.backend.event.comment.exceptions;

public class AdminReplyAlreadyExistsException extends RuntimeException {
    public AdminReplyAlreadyExistsException(String message) {
        super(message);
    }
}
