package nl.hackyourfuture.project.backend.event.similarity.service;

import nl.hackyourfuture.project.backend.event.category.model.Category;
import nl.hackyourfuture.project.backend.event.dto.response.EventSummaryResponse;
import nl.hackyourfuture.project.backend.event.exceptions.EventNotFoundException;
import nl.hackyourfuture.project.backend.event.model.EventSummary;
import nl.hackyourfuture.project.backend.event.similarity.model.SimilarEventCandidate;
import nl.hackyourfuture.project.backend.event.similarity.repository.EventSimilarityRepository;
import nl.hackyourfuture.project.backend.user.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventSimilarityServiceTest {

    @Mock
    private EventSimilarityRepository eventSimilarityRepository;

    @InjectMocks
    private EventSimilarityService eventSimilarityService;

    @Test
    void findSimilarEvents_whenSourceNotPublished_throwsEventNotFoundException() {
        UUID eventId = UUID.randomUUID();
        when(eventSimilarityRepository.existsPublishedEvent(eventId)).thenReturn(false);

        assertThrows(
                EventNotFoundException.class,
                () -> eventSimilarityService.findSimilarEvents(eventId, 5)
        );

        verify(eventSimilarityRepository, never()).findSimilarEvents(any(), anyInt());
    }

    @Test
    void findSimilarEvents_whenLimitBelowOne_throwsBadRequestException() {
        UUID eventId = UUID.randomUUID();

        assertThrows(
                BadRequestException.class,
                () -> eventSimilarityService.findSimilarEvents(eventId, 0)
        );

        verify(eventSimilarityRepository, never()).existsPublishedEvent(any());
    }

    @Test
    void findSimilarEvents_whenLimitAboveFifty_throwsBadRequestException() {
        UUID eventId = UUID.randomUUID();

        assertThrows(
                BadRequestException.class,
                () -> eventSimilarityService.findSimilarEvents(eventId, 51)
        );

        verify(eventSimilarityRepository, never()).existsPublishedEvent(any());
    }

    @Test
    void findSimilarEvents_whenCandidatesExist_mapsToSummariesAndDropsScore() {
        UUID sourceEventId = UUID.randomUUID();
        EventSummary summary = sampleSummary(UUID.randomUUID(), "Similar concert");
        SimilarEventCandidate candidate = new SimilarEventCandidate(summary, 0.92);

        when(eventSimilarityRepository.existsPublishedEvent(sourceEventId)).thenReturn(true);
        when(eventSimilarityRepository.findSimilarEvents(sourceEventId, 3))
                .thenReturn(List.of(candidate));

        List<EventSummaryResponse> result =
                eventSimilarityService.findSimilarEvents(sourceEventId, 3);

        assertEquals(1, result.size());
        assertEquals(summary.id(), result.getFirst().id());
        assertEquals(summary.title(), result.getFirst().title());
        assertEquals(summary.cityName(), result.getFirst().cityName());
        verify(eventSimilarityRepository).findSimilarEvents(eq(sourceEventId), eq(3));
    }

    private static EventSummary sampleSummary(UUID id, String title) {
        return new EventSummary(
                id,
                title,
                List.of(new Category(UUID.randomUUID(), "Music")),
                OffsetDateTime.parse("2026-09-12T17:00:00Z"),
                OffsetDateTime.parse("2026-09-12T21:00:00Z"),
                new BigDecimal("20.00"),
                "Damrak",
                "1",
                "1012 LG",
                "Amsterdam",
                "North Holland",
                new BigDecimal("52.367600"),
                new BigDecimal("4.904100"),
                "https://example.com/image.jpg",
                12L,
                false
        );
    }
}
