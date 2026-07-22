package com.khamphaviet.restaurant.deposit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name="reservation_deposits") @Getter @NoArgsConstructor
public class ReservationDeposit {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true) private Long reservationId;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private DepositStatus status;
    @Enumerated(EnumType.STRING) private DepositMethod method;
    private Instant paidAt;
    private String providerOrderId;
    private String providerCaptureId;
    @Version private Long version;

    public ReservationDeposit(Long reservationId, BigDecimal amount) {
        this.reservationId=reservationId; this.amount=amount; this.status=DepositStatus.PENDING;
    }
    public void pay(DepositMethod method, String orderId, String captureId) {
        if(status==DepositStatus.PAID) return;
        this.status=DepositStatus.PAID; this.method=method; this.paidAt=Instant.now();
        this.providerOrderId=orderId; this.providerCaptureId=captureId;
    }
}
