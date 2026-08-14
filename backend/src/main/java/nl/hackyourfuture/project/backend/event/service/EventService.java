package nl.hackyourfuture.project.backend.event.service;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.dto.EventResponse;
import nl.hackyourfuture.project.backend.event.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    public List<EventResponse> getAllEvents() {
        return eventRepository
                .getAllEvents()
                .stream()
                .map(EventResponse::from)
                .toList();
    }
}
