package com.khamphaviet.restaurant.reservation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "reservation_status_events")
@Getter
@NoArgsConstructor
public class ReservationStatusEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long reservationId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus fromStatus;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus toStatus;
    @Column(nullable = false, length = 180)
    private String actor;
    @Column(nullable = false, length = 400)
    private String reason;
    @Column(nullable = false)
    private Instant createdAt;

    public ReservationStatusEvent(Long reservationId, ReservationStatus fromStatus,
                                  ReservationStatus toStatus, String actor, String reason) {
        this.reservationId = reservationId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actor = actor;
        this.reason = reason;
        this.createdAt = Instant.now();
    }
}
