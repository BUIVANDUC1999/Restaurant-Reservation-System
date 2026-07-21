package com.khamphaviet.restaurant.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class DiningOrderDtos {
    private DiningOrderDtos() {}
    public record ItemRequest(@NotNull Long menuItemId, @Min(1) @Max(50) int quantity) {}
    public record CreateRequest(@NotEmpty @Size(max = 30) List<@Valid ItemRequest> items, @Size(max = 500) String note) {}
    public record StatusRequest(@NotNull DiningOrderStatus status) {}
    public record ItemResponse(Long id, Long menuItemId, String itemName, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {}
    public record OrderResponse(Long id, Long serviceSessionId, Long reservationId, String reservationCode, String customerName,
                                List<String> tableCodes, DiningOrderStatus status, String note, Instant createdAt,
                                Instant updatedAt, List<ItemResponse> items, BigDecimal total) {}
}
