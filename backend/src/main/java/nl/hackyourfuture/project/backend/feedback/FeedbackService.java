package nl.hackyourfuture.project.backend.feedback;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.feedback.dto.FeedbackPageResponse;
import nl.hackyourfuture.project.backend.feedback.dto.FeedbackResponse;
import nl.hackyourfuture.project.backend.feedback.dto.PatchFeedbackRequest;
import nl.hackyourfuture.project.backend.feedback.dto.PostFeedbackRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedbackService {
  private final FeedbackRepository feedbackRepository;

  public void submitFeedback (PostFeedbackRequest request){
    Feedback newFeedback = Feedback.builder()
        .topic(request.topic())
        .eventTitle(request.eventTitle())
        .rating(request.rating())
        .message(request.message())
        .senderName(request.senderName())
        .senderEmail(request.senderEmail())
        .build();

    feedbackRepository.createFeedback(newFeedback);

  }

  public FeedbackPageResponse getFeedbackPage(int page, int size){
    int offset = page*size;

    List<FeedbackResponse> feedbacks = feedbackRepository
        .getAllFeedbacks(size, offset)
        .stream()
        .map(FeedbackResponse::from)
        .toList();

    long totalElements = feedbackRepository.countFeedbacks();
    int totalPages = (int) Math.ceil((double) totalElements/size);
    boolean hasNext = page + 1 < totalPages;

    return new FeedbackPageResponse(feedbacks, page, size, totalElements, totalPages, hasNext);
  }

  public FeedbackResponse updateReviewedStatus(UUID id, PatchFeedbackRequest request){
    feedbackRepository.findFeedbackById(id)
        .orElseThrow(() -> new FeedbackNotFoundException("Feedback not found"));

    Feedback updated = Feedback.builder()
        .id(id)
        .isReviewed(request.isReviewed())
        .build();

    Feedback saved = feedbackRepository.updateReviewedStatusOfFeedback(updated);
    return FeedbackResponse.from(saved);
  }
}
