package com.khamphaviet.restaurant.reservation;

import com.khamphaviet.restaurant.deposit.DepositMethod;
import com.khamphaviet.restaurant.deposit.DepositStatus;
import com.khamphaviet.restaurant.table.TableStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class ReservationDtos {
    private ReservationDtos() {}

    public record CreateRequest(
            @NotBlank @Size(max = 120) String customerName,
            @NotBlank @Pattern(regexp = "^[0-9+ ]{9,15}$") String phone,
            @Email String email,
            @NotNull @FutureOrPresent LocalDate reservationDate,
            @NotBlank @Pattern(regexp = "LUNCH|DINNER") String timeSlot,
            LocalTime reservationTime,
            @Min(60) @Max(300) Integer durationMinutes,
            @NotNull @Min(1) @Max(300) Integer partySize,
            String preferredFloor,
            @Size(max = 1000) String note,
            @Size(max = 30) List<PreOrderItemRequest> preOrderItems,
            @Size(max = 8) List<@NotNull Long> selectedTableIds,
            Boolean notifyEmail,
            Boolean notifySms) {}

    public record PreOrderItemRequest(@NotNull Long menuItemId, @NotNull @Min(1) @Max(20) Integer quantity) {}
    public record PreOrderItemResponse(Long id, Long menuItemId, String itemName, BigDecimal unitPrice,
                                       Integer quantity, PreOrderStatus status, BigDecimal lineTotal) {}
    public record AssignedTableResponse(Long id, String code, String name, String floor, String area,
                                        Integer seats, TableStatus status, Integer layoutX, Integer layoutY, String shape) {}
    public record AssignTablesRequest(@NotEmpty List<@NotNull Long> tableIds) {}
    public record ReservationResponse(
            Long id, String code, String customerName, String phone, String email,
            LocalDate reservationDate, String timeSlot, Integer partySize,
            LocalTime reservationTime, Integer durationMinutes, Instant holdExpiresAt,
            String preferredFloor, String note, ReservationStatus status,
            Instant createdAt, Instant confirmedAt, Instant checkedInAt, Instant completedAt,
            boolean notifyEmail, boolean notifySms,
            List<PreOrderItemResponse> preOrderItems, List<AssignedTableResponse> assignedTables,
            Long serviceSessionId, long openOrderCount, boolean paid,
            BigDecimal depositAmount, DepositStatus depositStatus, DepositMethod depositMethod, Instant depositPaidAt) {}
    public record AvailabilityResponse(boolean available, int remainingSeats, String message) {}
    public record AvailableTableResponse(Long id, String code, String name, String area, Integer seats,
                                         Integer layoutX, Integer layoutY, String shape) {}
    public record StatusRequest(@NotNull ReservationStatus status) {}
}
