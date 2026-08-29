package nl.hackyourfuture.project.backend.feedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nl.hackyourfuture.project.backend.feedback.dto.Topic;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Feedback {
  private UUID id;
  private Topic topic;
  private String eventTitle;
  private int rating;
  private String message;
  private String senderName;
  private String senderEmail;
  private boolean isReviewed;
  private OffsetDateTime createdAt;
}
