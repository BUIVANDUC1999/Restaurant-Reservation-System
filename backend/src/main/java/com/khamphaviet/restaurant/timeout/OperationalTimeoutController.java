package com.khamphaviet.restaurant.timeout;

import org.springframework.web.bind.annotation.*;
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
    @PatchMapping("/{id}/resolve") public OperationalTimeout resolve(@PathVariable Long id, @RequestBody(required = false) ResolveRequest request) {
        return service.resolve(id, request == null ? null : request.note());
    }
    public record ResolveRequest(String note) {}
}
