package com.khamphaviet.restaurant.deposit;

import com.khamphaviet.restaurant.common.BusinessException;
import com.khamphaviet.restaurant.notification.NotificationService;
import com.khamphaviet.restaurant.reservation.Reservation;
import com.khamphaviet.restaurant.reservation.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class ReservationDepositService {
    private final ReservationDepositRepository deposits;
    private final ReservationRepository reservations;
    private final NotificationService notifications;

    public ReservationDepositService(ReservationDepositRepository deposits,ReservationRepository reservations,
                                     NotificationService notifications){
        this.deposits=deposits;this.reservations=reservations;this.notifications=notifications;
    }
    public record DepositResponse(BigDecimal amount,DepositStatus status,DepositMethod method,java.time.Instant paidAt){}
    public ReservationDeposit verified(String code,String phone){
        Reservation reservation=reservations.findByCodeIgnoreCaseAndPhone(code.trim(),phone.trim())
                .orElseThrow(()->new BusinessException("Không tìm thấy đơn đặt bàn"));
        return deposits.findByReservationId(reservation.getId()).orElseThrow(()->new BusinessException("Không tìm thấy khoản đặt cọc"));
    }
    public DepositResponse response(ReservationDeposit value){return new DepositResponse(value.getAmount(),value.getStatus(),value.getMethod(),value.getPaidAt());}
    @Transactional public DepositResponse completePayPal(Long reservationId,String orderId,String captureId){
        ReservationDeposit value=deposits.findByReservationId(reservationId).orElseThrow(()->new BusinessException("Không tìm thấy khoản đặt cọc"));
        value.pay(DepositMethod.PAYPAL,orderId,captureId);
        reservations.findById(reservationId).ifPresent(r->notifications.depositPaid(r,"PayPal Sandbox"));return response(value);
    }
}
