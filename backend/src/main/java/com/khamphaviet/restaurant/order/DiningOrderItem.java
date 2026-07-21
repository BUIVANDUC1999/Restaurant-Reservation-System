package com.khamphaviet.restaurant.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "dining_order_items")
@Getter
@NoArgsConstructor
public class DiningOrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long orderId;
    @Column(nullable = false) private Long menuItemId;
    @Column(nullable = false, length = 200) private String itemNameSnapshot;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false) private int quantity;

    public DiningOrderItem(Long orderId, Long menuItemId, String itemNameSnapshot, BigDecimal unitPrice, int quantity) {
        this.orderId = orderId; this.menuItemId = menuItemId; this.itemNameSnapshot = itemNameSnapshot;
        this.unitPrice = unitPrice; this.quantity = quantity;
    }
}
