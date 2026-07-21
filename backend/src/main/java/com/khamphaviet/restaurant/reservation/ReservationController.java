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
    @PostMapping("/reservations") public Reservation create(@Valid @RequestBody ReservationDtos.CreateRequest request) { return service.create(request); }
    @GetMapping("/reservations/lookup") public Reservation lookup(@RequestParam String code, @RequestParam String phone) { return service.lookup(code, phone); }
    @GetMapping("/staff/reservations") public List<Reservation> list() { return service.list(); }
    @PatchMapping("/staff/reservations/{id}/status") public Reservation status(@PathVariable Long id, @Valid @RequestBody ReservationDtos.StatusRequest request) { return service.updateStatus(id, request.status()); }
}

