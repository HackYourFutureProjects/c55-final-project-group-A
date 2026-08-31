package nl.hackyourfuture.project.backend.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import nl.hackyourfuture.project.backend.feedback.Topic;

@Schema(description = "The details needed to submit feedback")
public record PostFeedbackRequest (
    @NotNull(message = "Please specify what the feedback is about")
    @Schema(description = "What the feedback is about, either 'app' or 'event'", example = "app")
    Topic topic,

    @Size(max = 255, message = "Event title must not exceed 255 characters")
    @Schema(description = "Title of the event this feedback is about, if applicable", example = "Amsterdam Music Night")
    String eventTitle,


    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    @Schema(description = "Rating from 1 (worst) to 5 (best)", example = "4")
    int rating,

    @Size(max = 3000, message = "Message must not exceed 3000 characters")
    @Schema(description = "Free-text feedback message", example = "Loved the venue, but the sound was a bit too loud")
    String message,

    @Size(max = 150, message = "Name must not exceed 150 characters")
    @Schema(description = "Name of the person submitting the feedback", example = "Anouk de Vries")
    String senderName,

    @Email(message = "Please provide a valid email address")
    @Schema(description = "Email of the person submitting the feedback, in case they'd like a reply", example = "anouk.devries@example.com")
    String senderEmail
){
}
