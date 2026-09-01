package nl.hackyourfuture.project.backend.notification.model;

public record OutboxPayload(String eventTitle,
                            String linkPath ) {
}
