package nl.hackyourfuture.project.backend.event.service;

import nl.hackyourfuture.project.backend.event.exceptions.EventNotFoundException;
import nl.hackyourfuture.project.backend.event.model.EventSort;
import nl.hackyourfuture.project.backend.event.repository.EventRepository;
import nl.hackyourfuture.project.backend.user.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void getEventPage_whenDateFilterIncomplete_throwsBadRequestException() {
        assertThrows(
                BadRequestException.class,
                () -> eventService.getEventPage(
                        null,
                        null,
                        LocalDate.of(2026, 9, 1),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        EventSort.START_TIME_ASC,
                        0,
                        9
                )
        );

        verify(eventRepository, never()).findEventSummaries(any(), anyInt(), anyInt());
    }

    @Test
    void getEventPage_whenDateFilterInverted_throwsBadRequestException() {
        assertThrows(
                BadRequestException.class,
                () -> eventService.getEventPage(
                        null,
                        null,
                        LocalDate.of(2026, 9, 30),
                        LocalDate.of(2026, 9, 1),
                        null,
                        null,
                        null,
                        null,
                        null,
                        EventSort.START_TIME_ASC,
                        0,
                        9
                )
        );

        verify(eventRepository, never()).findEventSummaries(any(), anyInt(), anyInt());
    }

    @Test
    void getEventPage_whenLocationFilterIncomplete_throwsBadRequestException() {
        assertThrows(
                BadRequestException.class,
                () -> eventService.getEventPage(
                        null,
                        null,
                        null,
                        null,
                        new BigDecimal("52.3676"),
                        null,
                        null,
                        null,
                        null,
                        EventSort.START_TIME_ASC,
                        0,
                        9
                )
        );

        verify(eventRepository, never()).findEventSummaries(any(), anyInt(), anyInt());
    }

    @Test
    void getEventDetail_whenMissing_throwsEventNotFoundException() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findEventDetailById(eventId)).thenReturn(Optional.empty());

        assertThrows(
                EventNotFoundException.class,
                () -> eventService.getEventDetail(eventId)
        );
    }
}
