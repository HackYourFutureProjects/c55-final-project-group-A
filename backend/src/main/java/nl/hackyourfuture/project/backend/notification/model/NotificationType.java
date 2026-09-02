package nl.hackyourfuture.project.backend.notification.model;

import com.fasterxml.jackson.annotation.JsonValue;


public enum NotificationType {
    EVENT_CANCELLED,
    EVENT_UPDATED,
    EVENT_REMINDER,
    COMMENT_REPLY,
    NEW_FEEDBACK;

    public static NotificationType fromDbValue(String value) {
        return NotificationType.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toDbValue() {
        return name().toUpperCase();
    }
}