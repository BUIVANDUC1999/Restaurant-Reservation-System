package com.khamphaviet.restaurant.walkin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "walk_in_visits")
@Getter @NoArgsConstructor
public class WalkInVisit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @Column(nullable = false, unique = true, length = 24) private String code;
    @Column(nullable = false, length = 120) private String customerName;
    @Column(length = 20) private String phone;
    @Column(nullable = false) private Integer partySize;
    @Column(length = 100) private String areaPreference;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private WalkInPriority priority;
    @Column(length = 300) private String priorityReason;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private WalkInStatus status;
    @Column(nullable = false) private Instant arrivedAt;
    @Column(nullable = false) private Integer quotedWaitMinutes;
    @Column(nullable = false) private Instant expectedSeatAt;
    private Instant offeredAt;
    private Instant offerExpiresAt;
    private Instant seatedAt;
    private Instant paymentRequestedAt;
    private Instant cleaningStartedAt;
    private Instant completedAt;
    private Instant leftAt;
    private Long tableId;
    private Long reservationId;
    @Column(nullable = false) private Integer callCount;
    @Column(length = 500) private String note;

    public WalkInVisit(String code, String customerName, String phone, int partySize, String areaPreference,
                       WalkInPriority priority, String priorityReason, int quotedWaitMinutes, String note) {
        this.code=code; this.customerName=customerName; this.phone=phone; this.partySize=partySize;
        this.areaPreference=blankToNull(areaPreference); this.priority=priority; this.priorityReason=blankToNull(priorityReason);
        this.status=WalkInStatus.WAITING; this.arrivedAt=Instant.now(); this.quotedWaitMinutes=quotedWaitMinutes;
        this.expectedSeatAt=this.arrivedAt.plusSeconds(quotedWaitMinutes*60L); this.callCount=0; this.note=blankToNull(note);
    }

    public void reviseQuote(int minutes) {
        this.quotedWaitMinutes=minutes; this.expectedSeatAt=Instant.now().plusSeconds(minutes*60L);
    }
    public void offer(Long tableId, Long reservationId, int expiryMinutes) {
        this.status=WalkInStatus.TABLE_OFFERED; this.tableId=tableId; this.reservationId=reservationId;
        this.offeredAt=Instant.now(); this.offerExpiresAt=offeredAt.plusSeconds(expiryMinutes*60L); this.callCount++;
    }
    public void callAgain(int expiryMinutes) {
        this.offeredAt=Instant.now(); this.offerExpiresAt=offeredAt.plusSeconds(expiryMinutes*60L); this.callCount++;
    }
    public void seat() { this.status=WalkInStatus.SEATED; this.seatedAt=Instant.now(); }
    public void dining() { this.status=WalkInStatus.DINING; }
    public void requestPayment() { this.status=WalkInStatus.PAYMENT_REQUESTED; this.paymentRequestedAt=Instant.now(); }
    public void cleaning() { this.status=WalkInStatus.CLEANING; this.cleaningStartedAt=Instant.now(); }
    public void complete() { this.status=WalkInStatus.COMPLETED; this.completedAt=Instant.now(); }
    public void exit(WalkInStatus terminal) { this.status=terminal; this.leftAt=Instant.now(); }
    public void requeue(int quotedMinutes) {
        this.status=WalkInStatus.WAITING; this.tableId=null; this.reservationId=null;
        this.offeredAt=null; this.offerExpiresAt=null; reviseQuote(quotedMinutes);
    }
    private String blankToNull(String value){return value==null||value.isBlank()?null:value.trim();}
}
