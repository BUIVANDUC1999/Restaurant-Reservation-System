package com.khamphaviet.restaurant.billing;
import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.time.Instant;import java.util.List;
public final class CheckoutDtos{private CheckoutDtos(){}
 public record PayRequest(@NotNull PaymentMethod method,@NotNull @DecimalMin("0") BigDecimal discountAmount){}
 public record Line(String itemName,int quantity,BigDecimal unitPrice,BigDecimal lineTotal){}
 public record Checkout(Long serviceSessionId,Long reservationId,String reservationCode,String customerName,List<String> tableCodes,int partySize,List<Line> items,BigDecimal subtotal,long openOrderCount,boolean paid,Long paymentId,String invoiceCode,BigDecimal discountAmount,BigDecimal totalAmount,PaymentMethod paymentMethod,Instant paidAt){}
}
