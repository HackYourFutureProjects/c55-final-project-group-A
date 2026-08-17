package nl.hackyourfuture.project.backend.event.dto.response;

import java.util.List;

public record EventPageResponse(List<EventSummaryResponse> events,
                                int page,
                                int size,
                                long totalElements,
                                int totalPages,
                                boolean hasNext) {
}
