package com.khamphaviet.restaurant.walkin;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class WalkInDtos {
    private WalkInDtos(){}
    public record CreateRequest(
            @NotBlank @Size(max=120) String customerName,
            @Pattern(regexp="^[0-9+ ]{9,15}$") String phone,
            @NotNull @Min(1) @Max(30) Integer partySize,
            @Size(max=100) String areaPreference,
            WalkInPriority priority,
            @Size(max=300) String priorityReason,
            @Min(0) @Max(240) Integer quotedWaitMinutes,
            @Size(max=500) String note) {}
    public record OfferRequest(@NotNull Long tableId,@Size(max=500) String note) {}
    public record ActionRequest(@Size(max=500) String note,@Min(0)@Max(240) Integer quotedWaitMinutes) {}
    public record SuggestedTable(Long id,String code,String name,String area,int seats,int seatWaste,
                                 Instant availableAt,Instant nextOnlineReservationAt,boolean safe,String reason) {}
    public record EventResponse(Long id,WalkInStatus fromStatus,WalkInStatus toStatus,String action,
                                String note,String actor,Instant createdAt) {}
    public record VisitResponse(Long id,String code,String customerName,String phone,int partySize,String areaPreference,
                                WalkInPriority priority,String priorityReason,WalkInStatus status,WalkInSlaLevel slaLevel,
                                String slaMessage,Instant slaDeadlineAt,long elapsedMinutes,int quotedWaitMinutes,
                                Instant expectedSeatAt,Instant offeredAt,Instant offerExpiresAt,Instant seatedAt,
                                Instant paymentRequestedAt,Instant cleaningStartedAt,Instant completedAt,
                                Long tableId,String tableCode,Long reservationId,Long serviceSessionId,int callCount,String note,
                                List<SuggestedTable> suggestedTables,List<EventResponse> events) {}
    public record MetricsResponse(long totalVisits,long activeVisits,double averageWaitMinutes,long p90WaitMinutes,
                                  double quoteAccuracyPercent,double abandonmentPercent,double averageCleaningMinutes) {}
}
