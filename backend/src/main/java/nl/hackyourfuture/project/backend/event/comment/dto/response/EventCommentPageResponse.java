package nl.hackyourfuture.project.backend.event.comment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "A paginated list of event comments")
public record EventCommentPageResponse(

        @Schema(
                description = "Comments returned for the current page",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        List<EventCommentResponse> comments,

        @Schema(
                description = "Total number of comments for the event",
                example = "24",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        long totalComments,

        @Schema(
                description = "Whether more comments are available",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean hasMore
) {
}