package com.khamphaviet.restaurant.service;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "waiter_assignment_events")
@Getter
@NoArgsConstructor
public class WaiterAssignmentEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long serviceSessionId;
    @Column(nullable = false) private Long reservationId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private WaiterAssignmentAction action;
    private Long fromStaffId;
    @Column(length = 120) private String fromStaffName;
    private Long toStaffId;
    @Column(length = 120) private String toStaffName;
    @Column(nullable = false, length = 180) private String actor;
    @Column(nullable = false, length = 400) private String reason;
    @Column(nullable = false) private Instant createdAt;

    public WaiterAssignmentEvent(ServiceSession session, WaiterAssignmentAction action,
            Long fromStaffId, String fromStaffName, Long toStaffId, String toStaffName,
            String actor, String reason) {
        this.serviceSessionId = session.getId();
        this.reservationId = session.getReservationId();
        this.action = action;
        this.fromStaffId = fromStaffId;
        this.fromStaffName = fromStaffName;
        this.toStaffId = toStaffId;
        this.toStaffName = toStaffName;
        this.actor = actor;
        this.reason = reason;
        this.createdAt = Instant.now();
    }
}
