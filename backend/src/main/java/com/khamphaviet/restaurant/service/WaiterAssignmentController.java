package com.khamphaviet.restaurant.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff/service-sessions")
public class WaiterAssignmentController {
    private final WaiterAssignmentService assignments;

    public WaiterAssignmentController(WaiterAssignmentService assignments) {
        this.assignments = assignments;
    }

    public record AssignRequest(@NotNull Long staffId, @Size(max = 400) String reason) {}
    public record ReasonRequest(@Size(max = 400) String reason) {}

    @GetMapping("/waiters")
    public List<WaiterAssignmentService.WaiterSummary> waiters() {
        return assignments.waiters();
    }

    @GetMapping("/waiter-history")
    public List<WaiterAssignmentService.AssignmentEventResponse> history(
            @RequestParam(required = false) Long sessionId) {
        return assignments.history(sessionId);
    }

    @PostMapping("/{sessionId}/claim")
    public WaiterAssignmentService.Assignment claim(@PathVariable Long sessionId,
            @RequestBody(required = false) @Valid ReasonRequest request, Authentication authentication) {
        return assignments.claim(sessionId, authentication.getName(), request == null ? null : request.reason());
    }

    @PutMapping("/{sessionId}/waiter")
    public WaiterAssignmentService.Assignment assign(@PathVariable Long sessionId,
            @Valid @RequestBody AssignRequest request, Authentication authentication) {
        return assignments.assign(sessionId, request.staffId(), authentication.getName(), request.reason());
    }

    @DeleteMapping("/{sessionId}/waiter")
    public WaiterAssignmentService.Assignment unassign(@PathVariable Long sessionId,
            @RequestBody(required = false) @Valid ReasonRequest request, Authentication authentication) {
        return assignments.unassign(sessionId, authentication.getName(), request == null ? null : request.reason());
    }
}
