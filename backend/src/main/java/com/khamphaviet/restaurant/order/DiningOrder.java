package com.khamphaviet.restaurant.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "dining_orders")
@Getter
@NoArgsConstructor
public class DiningOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long serviceSessionId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private DiningOrderStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private OrderSource source;
    @Column(length = 500) private String note;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    public DiningOrder(Long serviceSessionId, String note) { this(serviceSessionId, note, OrderSource.TABLE_ORDER); }

    public DiningOrder(Long serviceSessionId, String note, OrderSource source) {
        this.serviceSessionId = serviceSessionId;
        this.note = note == null || note.isBlank() ? null : note.trim();
        this.source = source;
        this.status = DiningOrderStatus.SUBMITTED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void changeStatus(DiningOrderStatus next) {
        this.status = next;
        this.updatedAt = Instant.now();
    }
}
