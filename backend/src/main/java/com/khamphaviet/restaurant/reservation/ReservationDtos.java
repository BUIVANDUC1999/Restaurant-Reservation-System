package com.khamphaviet.restaurant.reservation;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import com.khamphaviet.restaurant.table.TableStatus;

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
            @Size(max = 1000) String note,
            @Size(max = 30) List<PreOrderItemRequest> preOrderItems) {}
    public record PreOrderItemRequest(@NotNull Long menuItemId, @NotNull @Min(1) @Max(20) Integer quantity) {}
    public record PreOrderItemResponse(Long id, Long menuItemId, String itemName, BigDecimal unitPrice,
                                       Integer quantity, PreOrderStatus status, BigDecimal lineTotal) {}
    public record AssignedTableResponse(Long id, String code, String name, String floor, String area,
                                        Integer seats, TableStatus status) {}
    public record AssignTablesRequest(@NotEmpty List<@NotNull Long> tableIds) {}
    public record ReservationResponse(Long id, String code, String customerName, String phone, String email,
                                      LocalDate reservationDate, String timeSlot, Integer partySize,
                                      String preferredFloor, String note, ReservationStatus status,
                                      Instant createdAt, List<PreOrderItemResponse> preOrderItems,
                                      List<AssignedTableResponse> assignedTables, Long serviceSessionId, long openOrderCount, boolean paid) {}
    public record AvailabilityResponse(boolean available, int remainingSeats, String message) {}
    public record StatusRequest(@NotNull ReservationStatus status) {}
}
