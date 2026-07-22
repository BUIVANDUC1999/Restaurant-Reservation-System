package com.khamphaviet.restaurant.reservation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    @Column(nullable = false) private Integer partySize;
    private String preferredFloor;
    @Column(length = 1000) private String note;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReservationStatus status;
    @Column(nullable = false) private Instant createdAt;

    public Reservation(String code, String customerName, String phone, String email, LocalDate reservationDate,
                       String timeSlot, Integer partySize, String preferredFloor, String note) {
        this.code = code; this.customerName = customerName; this.phone = phone; this.email = email;
        this.reservationDate = reservationDate; this.timeSlot = timeSlot; this.partySize = partySize;
        this.preferredFloor = preferredFloor; this.note = note; this.status = ReservationStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void changeStatus(ReservationStatus status) { this.status = status; }
}

