package nl.hackyourfuture.project.backend.event.comment.exceptions;

public class AdminReplyNotFoundException extends RuntimeException {
    public AdminReplyNotFoundException(String message) {
        super(message);
    }
}
