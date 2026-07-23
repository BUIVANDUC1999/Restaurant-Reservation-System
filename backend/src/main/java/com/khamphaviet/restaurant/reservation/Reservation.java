package com.khamphaviet.restaurant.reservation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @Column(nullable = false, unique = true, length = 16) private String code;
    @Column(nullable = false) private String customerName;
    @Column(nullable = false, length = 20) private String phone;
    private String email;
    @Column(nullable = false) private LocalDate reservationDate;
    @Column(nullable = false, length = 20) private String timeSlot;
    @Column(nullable = false) private LocalTime reservationTime;
    @Column(nullable = false) private Integer durationMinutes;
    @Column(nullable = false) private Integer partySize;
    private String preferredFloor;
    @Column(length = 1000) private String note;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReservationStatus status;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant holdExpiresAt;
    private Instant confirmedAt;
    private Instant checkedInAt;
    private Instant completedAt;
    private Instant cancelledAt;
    @Column(nullable = false) private boolean notifyEmail;
    @Column(nullable = false) private boolean notifySms;

    public Reservation(String code, String customerName, String phone, String email, LocalDate reservationDate,
                       String timeSlot, LocalTime reservationTime, Integer durationMinutes, Integer partySize,
                       String preferredFloor, String note, boolean notifyEmail, boolean notifySms) {
        this.code = code; this.customerName = customerName; this.phone = phone; this.email = email;
        this.reservationDate = reservationDate; this.timeSlot = timeSlot; this.reservationTime = reservationTime;
        this.durationMinutes = durationMinutes; this.partySize = partySize;
        this.preferredFloor = preferredFloor; this.note = note; this.status = ReservationStatus.PENDING;
        this.createdAt = Instant.now();
        this.holdExpiresAt = this.createdAt.plusSeconds(600);
        this.notifyEmail = notifyEmail && email != null && !email.isBlank();
        this.notifySms = notifySms;
    }

    public void changeStatus(ReservationStatus status) {
        this.status = status;
        Instant now = Instant.now();
        if (status == ReservationStatus.CONFIRMED) this.confirmedAt = now;
        if (status == ReservationStatus.CHECKED_IN) this.checkedInAt = now;
        if (status == ReservationStatus.COMPLETED) this.completedAt = now;
        if (status == ReservationStatus.CANCELLED || status == ReservationStatus.REJECTED || status == ReservationStatus.NO_SHOW)
            this.cancelledAt = now;
    }

    public LocalTime effectiveTime() {
        return reservationTime == null ? ("LUNCH".equals(timeSlot) ? LocalTime.of(11, 0) : LocalTime.of(17, 30)) : reservationTime;
    }
    public int effectiveDurationMinutes() { return durationMinutes == null ? 120 : durationMinutes; }
}

