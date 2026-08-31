package nl.hackyourfuture.project.backend.feedback;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.feedback.dto.PostFeedbackRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@Tag(name = "Feedback", description = "Submitting and reviewing feedback about the app or events")
public class FeedbackController {

  private final FeedbackService feedbackService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Submit feedback", description = "Submits feedback about the app or a specific event. Available to anyone, no authentication required.")
  @ApiResponse(responseCode = "201", description = "Feedback submitted successfully")
  @ApiResponse(
      responseCode = "400",
      description = "The request body is invalid",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  public void submitFeedback(@Valid @RequestBody PostFeedbackRequest request) {
    feedbackService.submitFeedback(request);
  }

}
