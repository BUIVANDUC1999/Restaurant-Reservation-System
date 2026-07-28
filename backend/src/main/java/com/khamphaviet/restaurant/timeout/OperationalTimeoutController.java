package com.khamphaviet.restaurant.timeout;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.List;

@RestController
@RequestMapping("/api/v1/staff/timeouts")
public class OperationalTimeoutController {
    private final OperationalTimeoutService service;
    private final OperationalTimePolicy policy;
    public OperationalTimeoutController(OperationalTimeoutService service, OperationalTimePolicy policy) {
        this.service = service; this.policy = policy;
    }
    @GetMapping public List<OperationalTimeout> list() { return service.list(); }
    @GetMapping("/policy") public OperationalTimePolicy.Snapshot policy() { return policy.snapshot(); }
    @PatchMapping("/{id}/resolve") public OperationalTimeout resolve(@PathVariable Long id,
            @RequestBody(required = false) ResolveRequest request, Authentication auth) {
        return service.resolve(id, request == null ? null : request.note(), auth.getName());
    }
    @PatchMapping("/{id}/assign") public OperationalTimeout assign(@PathVariable Long id,
            @RequestBody AssignRequest request, Authentication auth) {
        return service.assign(id, request.assignee(), request.note(), auth.getName());
    }
    @PatchMapping("/{id}/acknowledge") public OperationalTimeout acknowledge(@PathVariable Long id, Authentication auth) {
        return service.acknowledge(id, auth.getName());
    }
    @GetMapping("/{id}/events") public List<OperationalTimeoutEvent> events(@PathVariable Long id) {
        return service.events(id);
    }
    public record ResolveRequest(String note) {}
    public record AssignRequest(String assignee, String note) {}
}
