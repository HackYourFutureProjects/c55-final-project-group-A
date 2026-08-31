package nl.hackyourfuture.project.backend.feedback;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.feedback.dto.FeedbackPageResponse;
import nl.hackyourfuture.project.backend.feedback.dto.FeedbackResponse;
import nl.hackyourfuture.project.backend.feedback.dto.PatchFeedbackRequest;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/feedback")
@RequiredArgsConstructor
@Tag(name = "Feedback")
public class AdminFeedbackController {

  private final FeedbackService feedbackService;

  @GetMapping
  @Operation(summary = "List feedback", description = "Returns a paginated list of all feedback submissions. Admin only.")
  @ApiResponse(responseCode = "200", description = "A page of feedback with pagination metadata")
  @ApiResponse(responseCode = "400",
      description = "Page or size is outside the allowed range",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  public FeedbackPageResponse getFeedback(
      @Parameter(description = "Zero-based page number", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,

      @Parameter(description = "Number of feedback per page, between 1 and 100", example = "9")
      @RequestParam(defaultValue = "9") @Min(1) @Max(100) int size
  ) {
    return feedbackService.getFeedbackPage(page, size);
  }

  @PatchMapping("/{id}")
  @Operation(summary = "Update review status", description = "Marks a feedback as reviewed or not reviewed. Admin only.")
  @ApiResponse(responseCode = "200", description = "The updated feedback")
  @ApiResponse(
      responseCode = "404",
      description = "Feedback not found",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  public FeedbackResponse updateReviewedStatus(
      @Parameter(description = "ID of the feedback to update", example = "40000000-0000-0000-0000-000000000001")
      @PathVariable UUID id,
      @Valid @RequestBody PatchFeedbackRequest request
  ) {
    return feedbackService.updateReviewedStatus(id, request);
  }
}
