package com.khamphaviet.restaurant.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;

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
    @Enumerated(EnumType.STRING) @Column(nullable=false) private DiningOrderItemStatus status;
    @Column(nullable=false) private Integer preparationMinutes;
    @Column(nullable=false) private Instant estimatedReadyAt;
    private Instant startedAt;
    private Instant delayedUntil;
    @Column(length=300) private String delayReason;
    private Instant readyAt;
    private Instant servedAt;

    public DiningOrderItem(Long orderId, Long menuItemId, String itemNameSnapshot, BigDecimal unitPrice, int quantity) {
        this(orderId,menuItemId,itemNameSnapshot,unitPrice,quantity,20);
    }
    public DiningOrderItem(Long orderId, Long menuItemId, String itemNameSnapshot, BigDecimal unitPrice, int quantity, int preparationMinutes) {
        this.orderId = orderId; this.menuItemId = menuItemId; this.itemNameSnapshot = itemNameSnapshot;
        this.unitPrice = unitPrice; this.quantity = quantity;
        this.status=DiningOrderItemStatus.SUBMITTED;this.preparationMinutes=preparationMinutes;
        this.estimatedReadyAt=Instant.now().plusSeconds(preparationMinutes*60L);
    }
    public void preparing(){if(status!=DiningOrderItemStatus.SUBMITTED)throw new IllegalStateException();status=DiningOrderItemStatus.PREPARING;startedAt=Instant.now();}
    public void delay(int minutes,String reason){if(status!=DiningOrderItemStatus.PREPARING&&status!=DiningOrderItemStatus.DELAYED)throw new IllegalStateException();status=DiningOrderItemStatus.DELAYED;delayedUntil=Instant.now().plusSeconds(minutes*60L);estimatedReadyAt=delayedUntil;delayReason=reason;}
    public void ready(){if(status!=DiningOrderItemStatus.PREPARING&&status!=DiningOrderItemStatus.DELAYED)throw new IllegalStateException();status=DiningOrderItemStatus.READY;readyAt=Instant.now();}
    public void served(){if(status!=DiningOrderItemStatus.READY)throw new IllegalStateException();status=DiningOrderItemStatus.SERVED;servedAt=Instant.now();}
}
