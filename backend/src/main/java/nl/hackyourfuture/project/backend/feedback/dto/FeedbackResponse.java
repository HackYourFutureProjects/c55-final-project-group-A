package nl.hackyourfuture.project.backend.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import nl.hackyourfuture.project.backend.feedback.Feedback;
import nl.hackyourfuture.project.backend.feedback.Topic;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Feedback as returned by the API")
public record FeedbackResponse(
    @Schema(
        description = "Unique identifier of the feedback",
        example = "40000000-0000-0000-0000-000000000001",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    UUID id,

    @Schema(
        description = "What the feedback is about, either 'app' or 'event'",
        example = "app",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    Topic topic,

    @Schema(
        description = "Title of the event this feedback is about, if applicable",
        example = "Amsterdam Music Night",
        nullable = true
    )
    String eventTitle,

    @Schema(
        description = "Rating from 1 (worst) to 5 (best)",
        example = "4",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    int rating,

    @Schema(
        description = "Free-text feedback message",
        example = "Loved the venue, but the sound was a bit too loud",
        nullable = true
    )
    String message,

    @Schema(
        description = "Name of the person who submitted the feedback",
        example = "Anouk de Vries",
        nullable = true
    )
    String senderName,

    @Schema(
        description = "Email of the person who submitted the feedback, in case they'd like a reply",
        example = "anouk.devries@example.com",
        nullable = true
    )
    String senderEmail,

    @Schema(
        description = "Whether an admin has reviewed this feedback",
        example = "false",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean isReviewed,

    @Schema(
        description = "Timestamp when the feedback was submitted",
        example = "2026-08-29T22:45:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    OffsetDateTime createdAt
) {
  public static FeedbackResponse from(Feedback feedback) {
    return new FeedbackResponse(
        feedback.getId(),
        feedback.getTopic(),
        feedback.getEventTitle(),
        feedback.getRating(),
        feedback.getMessage(),
        feedback.getSenderName(),
        feedback.getSenderEmail(),
        feedback.isReviewed(),
        feedback.getCreatedAt()
    );
  }
}
