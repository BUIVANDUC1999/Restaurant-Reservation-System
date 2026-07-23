package com.khamphaviet.restaurant.deposit;

import com.khamphaviet.restaurant.common.BusinessException;
import com.khamphaviet.restaurant.notification.NotificationService;
import com.khamphaviet.restaurant.reservation.Reservation;
import com.khamphaviet.restaurant.reservation.ReservationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class ReservationDepositService {
    private final ReservationDepositRepository deposits;
    private final ReservationRepository reservations;
    private final NotificationService notifications;
    private final String bankId,accountNo,accountName;

    public ReservationDepositService(ReservationDepositRepository deposits,ReservationRepository reservations,
                                     @Value("${app.deposit.qr.bank-id:}")String bankId,
                                     @Value("${app.deposit.qr.account-no:}")String accountNo,
                                     @Value("${app.deposit.qr.account-name:}")String accountName,
                                     NotificationService notifications){
        this.deposits=deposits;this.reservations=reservations;this.notifications=notifications;
        this.bankId=bankId;this.accountNo=accountNo;this.accountName=accountName;
    }
    public record DepositResponse(BigDecimal amount,DepositStatus status,DepositMethod method,java.time.Instant paidAt){}
    public record QrResponse(boolean enabled,String imageUrl,String bankId,String accountNo,String accountName,String transferContent,BigDecimal amount){}
    public ReservationDeposit verified(String code,String phone){
        Reservation reservation=reservations.findByCodeIgnoreCaseAndPhone(code.trim(),phone.trim())
                .orElseThrow(()->new BusinessException("Không tìm thấy đơn đặt bàn"));
        return deposits.findByReservationId(reservation.getId()).orElseThrow(()->new BusinessException("Không tìm thấy khoản đặt cọc"));
    }
    public DepositResponse response(ReservationDeposit value){return new DepositResponse(value.getAmount(),value.getStatus(),value.getMethod(),value.getPaidAt());}
    public QrResponse qr(String code,String phone){
        ReservationDeposit deposit=verified(code,phone);String content="COC "+code.toUpperCase();boolean enabled=!bankId.isBlank()&&!accountNo.isBlank();
        String image=enabled?"https://img.vietqr.io/image/"+url(bankId)+"-"+url(accountNo)+"-compact2.png?amount="+deposit.getAmount().toBigInteger()+"&addInfo="+url(content)+"&accountName="+url(accountName):"";
        return new QrResponse(enabled,image,bankId,accountNo,accountName,content,deposit.getAmount());
    }
    @Transactional public DepositResponse confirmQr(String code,String phone){
        ReservationDeposit value=verified(code,phone);value.pay(DepositMethod.QR,null,"QR-"+code);
        reservations.findById(value.getReservationId()).ifPresent(r->notifications.depositPaid(r,"QR"));return response(value);
    }
    @Transactional public DepositResponse completePayPal(Long reservationId,String orderId,String captureId){
        ReservationDeposit value=deposits.findByReservationId(reservationId).orElseThrow(()->new BusinessException("Không tìm thấy khoản đặt cọc"));
        value.pay(DepositMethod.PAYPAL,orderId,captureId);
        reservations.findById(reservationId).ifPresent(r->notifications.depositPaid(r,"PayPal Sandbox"));return response(value);
    }
    private String url(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
}
