package com.khamphaviet.restaurant.reservation;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ReservationController {
    private final ReservationService service;
    public ReservationController(ReservationService service) { this.service = service; }

    @GetMapping("/reservations/availability")
    public ReservationDtos.AvailabilityResponse availability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String timeSlot, @RequestParam int partySize) {
        return service.availability(date, timeSlot, partySize);
    }
    @PostMapping("/reservations") public ReservationDtos.ReservationResponse create(@Valid @RequestBody ReservationDtos.CreateRequest request) { return service.create(request); }
    @GetMapping("/reservations/lookup") public ReservationDtos.ReservationResponse lookup(@RequestParam String code, @RequestParam String phone) { return service.lookup(code, phone); }
    @GetMapping("/staff/reservations") public List<ReservationDtos.ReservationResponse> list() { return service.list(); }
    @PatchMapping("/staff/reservations/{id}/status") public ReservationDtos.ReservationResponse status(@PathVariable Long id, @Valid @RequestBody ReservationDtos.StatusRequest request) { return service.updateStatus(id, request.status()); }
    @PostMapping("/staff/reservations/{id}/preorder/confirm") public ReservationDtos.ReservationResponse confirmPreOrder(@PathVariable Long id) { return service.confirmPreOrder(id); }
    @PutMapping("/staff/reservations/{id}/tables") public ReservationDtos.ReservationResponse assignTables(@PathVariable Long id, @Valid @RequestBody ReservationDtos.AssignTablesRequest request) { return service.assignTables(id, request.tableIds()); }
    @PostMapping("/staff/reservations/{id}/check-in") public ReservationDtos.ReservationResponse checkIn(@PathVariable Long id) { return service.checkIn(id); }
    @PostMapping("/staff/reservations/{id}/complete") public ReservationDtos.ReservationResponse complete(@PathVariable Long id) { return service.completeService(id); }
}
