package com.khamphaviet.restaurant.reservation;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class ReservationDtos {
    private ReservationDtos() {}
    public record CreateRequest(
            @NotBlank @Size(max = 120) String customerName,
            @NotBlank @Pattern(regexp = "^[0-9+ ]{9,15}$") String phone,
            @Email String email,
            @NotNull @FutureOrPresent LocalDate reservationDate,
            @NotBlank @Pattern(regexp = "LUNCH|DINNER") String timeSlot,
            @NotNull @Min(1) @Max(300) Integer partySize,
            String preferredFloor,
            @Size(max = 1000) String note) {}
    public record AvailabilityResponse(boolean available, int remainingSeats, String message) {}
    public record StatusRequest(@NotNull ReservationStatus status) {}
}

