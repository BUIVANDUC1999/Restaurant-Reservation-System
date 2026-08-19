package com.khamphaviet.restaurant.demo;

import com.khamphaviet.restaurant.walkin.WalkInPriority;
import jakarta.validation.constraints.*;

public final class DemoScenarioDtos {
    private DemoScenarioDtos() {}

    public record CreateRequest(
            @NotNull DemoScenarioType type,
            @Size(max=120) String customerName,
            @Pattern(regexp="^$|^[0-9+ ]{9,15}$") String phone,
            @Email @Size(max=180) String email,
            Boolean notifyEmail,
            @Min(1) @Max(30) Integer partySize,
            @Size(max=100) String areaPreference,
            WalkInPriority priority,
            @Size(max=300) String priorityReason,
            @Min(1) @Max(240) Integer minutes,
            Long tableId,
            Long menuItemId,
            @Size(max=300) String reason,
            @Size(max=500) String note) {}

    public record CreateResponse(
            DemoScenarioType type,
            String group,
            String title,
            String message,
            String targetPath,
            Long entityId,
            Long reservationId,
            Long tableId) {}
}
