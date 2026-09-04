package nl.hackyourfuture.project.backend.event.controller;

import nl.hackyourfuture.project.backend.event.dto.response.EventPageResponse;
import nl.hackyourfuture.project.backend.event.model.EventSort;
import nl.hackyourfuture.project.backend.event.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class)
@AutoConfigureMockMvc(addFilters = false)
class EventControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    EventControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void getEvents_returnsOk() throws Exception {
        when(eventService.getEventPage(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(EventSort.START_TIME_ASC),
                eq(0),
                eq(9)
        )).thenReturn(new EventPageResponse(List.of(), 0, 9, 0, 0, false));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(9))
                .andExpect(jsonPath("$.events").isArray());
    }
}
