package com.khamphaviet.restaurant.service;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "service_sessions")
@Getter
@NoArgsConstructor
public class ServiceSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private Long reservationId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ServiceSessionStatus status;
    @Column(nullable = false) private Instant openedAt;
    private Instant closedAt;
    private Long assignedStaffId;
    @Column(length = 120) private String assignedStaffName;
    @Column(length = 180) private String assignedStaffEmail;
    @Column(length = 180) private String assignedBy;
    private Instant assignedAt;
    public ServiceSession(Long reservationId) { this.reservationId = reservationId; this.status = ServiceSessionStatus.ACTIVE; this.openedAt = Instant.now(); }
    public void complete() { this.status = ServiceSessionStatus.COMPLETED; this.closedAt = Instant.now(); }
    public void assignStaff(Long staffId, String staffName, String staffEmail, String actor) {
        this.assignedStaffId = staffId;
        this.assignedStaffName = staffName;
        this.assignedStaffEmail = staffEmail;
        this.assignedBy = actor;
        this.assignedAt = Instant.now();
    }
    public void clearStaff(String actor) {
        this.assignedStaffId = null;
        this.assignedStaffName = null;
        this.assignedStaffEmail = null;
        this.assignedBy = actor;
        this.assignedAt = Instant.now();
    }
}

