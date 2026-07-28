package com.khamphaviet.restaurant.operations;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/operations/events")
public class StaffEventController {
    private final StaffEventService events;
    public StaffEventController(StaffEventService events) { this.events = events; }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() { return events.subscribe(); }
}
