package com.khamphaviet.restaurant.reservation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "reservation_table_assignments", uniqueConstraints = @UniqueConstraint(columnNames = {"reservation_id", "table_id"}))
@Getter
@NoArgsConstructor
public class ReservationTableAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long reservationId;
    @Column(nullable = false) private Long tableId;
    @Column(nullable = false) private Instant assignedAt;
    public ReservationTableAssignment(Long reservationId, Long tableId) {
        this.reservationId = reservationId; this.tableId = tableId; this.assignedAt = Instant.now();
    }
}

