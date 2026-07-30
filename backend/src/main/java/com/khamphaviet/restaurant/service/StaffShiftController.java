package com.khamphaviet.restaurant.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/shifts")
public class StaffShiftController {
    private final StaffShiftService shifts;

    public StaffShiftController(StaffShiftService shifts) {
        this.shifts = shifts;
    }

    public record CreateRequest(@NotNull Long staffId, @NotNull Instant startsAt, @NotNull Instant endsAt) {}

    @GetMapping
    public List<StaffShiftService.ShiftResponse> today() {
        return shifts.today();
    }

    @PostMapping
    public StaffShiftService.ShiftResponse create(@Valid @RequestBody CreateRequest request,
            Authentication authentication) {
        return shifts.create(request.staffId(), request.startsAt(), request.endsAt(), authentication.getName());
    }

    @PostMapping("/{id}/start")
    public StaffShiftService.ShiftResponse start(@PathVariable Long id) {
        return shifts.start(id);
    }

    @PostMapping("/{id}/complete")
    public StaffShiftService.ShiftResponse complete(@PathVariable Long id) {
        return shifts.complete(id);
    }

    @PostMapping("/{id}/cancel")
    public StaffShiftService.ShiftResponse cancel(@PathVariable Long id) {
        return shifts.cancel(id);
    }
}
