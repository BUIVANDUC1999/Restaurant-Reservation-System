package com.khamphaviet.restaurant.reservation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "reservation_items")
@Getter
@NoArgsConstructor
public class ReservationItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long reservationId;
    @Column(nullable = false) private Long menuItemId;
    @Column(nullable = false) private String itemNameSnapshot;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false) private Integer quantity;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PreOrderStatus status;

    public ReservationItem(Long reservationId, Long menuItemId, String itemNameSnapshot, BigDecimal unitPrice, Integer quantity) {
        this.reservationId = reservationId; this.menuItemId = menuItemId; this.itemNameSnapshot = itemNameSnapshot;
        this.unitPrice = unitPrice; this.quantity = quantity; this.status = PreOrderStatus.REQUESTED;
    }
    public void confirm() { if (status == PreOrderStatus.REQUESTED) status = PreOrderStatus.CONFIRMED; }
}

