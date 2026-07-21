package com.khamphaviet.restaurant.billing;

import jakarta.persistence.*;import lombok.Getter;import lombok.NoArgsConstructor;import java.math.BigDecimal;import java.time.Instant;
@Entity @Table(name="payments") @Getter @NoArgsConstructor
public class Payment{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)private Long id;
 @Column(nullable=false,unique=true)private Long serviceSessionId;
 @Column(nullable=false,unique=true)private String invoiceCode;
 @Column(nullable=false,precision=12,scale=2)private BigDecimal subtotal;
 @Column(nullable=false,precision=12,scale=2)private BigDecimal discountAmount;
 @Column(nullable=false,precision=12,scale=2)private BigDecimal totalAmount;
 @Enumerated(EnumType.STRING)@Column(nullable=false)private PaymentMethod method;
 @Enumerated(EnumType.STRING)@Column(nullable=false)private PaymentStatus status;
 @Column(nullable=false)private Instant paidAt;
 private String providerOrderId;
 private String providerCaptureId;
 public Payment(Long sessionId,BigDecimal subtotal,BigDecimal discount,PaymentMethod method){this.serviceSessionId=sessionId;this.invoiceCode="HD-"+String.format("%06d",sessionId);this.subtotal=subtotal;this.discountAmount=discount;this.totalAmount=subtotal.subtract(discount);this.method=method;this.status=PaymentStatus.PAID;this.paidAt=Instant.now();}
 public Payment(Long sessionId,BigDecimal subtotal,BigDecimal discount,String providerOrderId,String providerCaptureId){this(sessionId,subtotal,discount,PaymentMethod.PAYPAL);this.providerOrderId=providerOrderId;this.providerCaptureId=providerCaptureId;}
}
