package com.khamphaviet.restaurant.notification;

import com.khamphaviet.restaurant.reservation.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NotificationService {
    private static final Logger log=LoggerFactory.getLogger(NotificationService.class);
    private static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    private final OperationalNotificationRepository notifications;
    private final ReservationRepository reservations;
    private final JavaMailSender mailSender;
    private final boolean emailEnabled;
    private final boolean smsSandbox;
    private final String mailFrom;

    public NotificationService(OperationalNotificationRepository notifications, ReservationRepository reservations,
                               JavaMailSender mailSender,
                               @Value("${app.notifications.email-enabled:false}") boolean emailEnabled,
                               @Value("${app.notifications.sms-sandbox:true}") boolean smsSandbox,
                               @Value("${spring.mail.username:no-reply@khamphaviet.local}") String mailFrom) {
        this.notifications=notifications;this.reservations=reservations;this.mailSender=mailSender;
        this.emailEnabled=emailEnabled;this.smsSandbox=smsSandbox;this.mailFrom=mailFrom;
    }

    @Transactional
    public void reservationCreated(Reservation r) {
        queue(r,NotificationType.NEW_RESERVATION,NotificationChannel.IN_APP,"NHÂN VIÊN",
                "Có lịch đặt bàn mới",summary(r),"staff-new-"+r.getId());
        String customer="Kính gửi "+r.getCustomerName()+", nhà hàng đã nhận yêu cầu "+r.getCode()+". "+summary(r)
                +". Bàn được giữ 10 phút để hoàn tất đặt cọc.";
        if(r.isNotifyEmail()&&r.getEmail()!=null) queue(r,NotificationType.NEW_RESERVATION,NotificationChannel.EMAIL,r.getEmail(),
                "Xác nhận yêu cầu đặt bàn "+r.getCode(),customer,"email-created-"+r.getId());
        if(r.isNotifySms()) queue(r,NotificationType.NEW_RESERVATION,NotificationChannel.SMS,r.getPhone(),
                "Xác nhận đặt bàn",customer,"sms-created-"+r.getId());
        deliverPending();
    }

    @Transactional
    public void depositPaid(Reservation r,String method) {
        String message="Đơn "+r.getCode()+" đã thanh toán đặt cọc bằng "+method+".";
        queue(r,NotificationType.DEPOSIT_PAID,NotificationChannel.IN_APP,"NHÂN VIÊN","Đã nhận tiền đặt cọc",message,"staff-deposit-"+r.getId());
        if(r.isNotifyEmail()&&r.getEmail()!=null)queue(r,NotificationType.DEPOSIT_PAID,NotificationChannel.EMAIL,r.getEmail(),
                "Đã nhận tiền đặt cọc "+r.getCode(),message,"email-deposit-"+r.getId());
        if(r.isNotifySms())queue(r,NotificationType.DEPOSIT_PAID,NotificationChannel.SMS,r.getPhone(),
                "Đã nhận tiền đặt cọc",message,"sms-deposit-"+r.getId());
        deliverPending();
    }

    @Scheduled(fixedDelay=60000)
    @Transactional
    public void scheduleReminders() {
        LocalDate today=LocalDate.now(ZONE); LocalDateTime now=LocalDateTime.now(ZONE);
        List<Reservation> active=reservations.findByReservationDateAndStatusIn(today,
                List.of(ReservationStatus.PENDING,ReservationStatus.CONFIRMED));
        for(Reservation r:active){
            LocalDateTime arrival=LocalDateTime.of(r.getReservationDate(),r.effectiveTime());
            long minutes=Duration.between(now,arrival).toMinutes();
            if(minutes>=29&&minutes<=30) alert(r,NotificationType.UPCOMING_30,"Khách sắp đến",
                    "Lịch "+r.getCode()+" sẽ đến sau khoảng 30 phút.");
            if(r.getStatus()==ReservationStatus.CONFIRMED&&minutes<=-15&&minutes>-20) alert(r,NotificationType.LATE_15,
                    "Khách trễ 15 phút","Lịch "+r.getCode()+" đã trễ 15 phút. Vui lòng xác nhận khách đang đến.");
            if(r.getStatus()==ReservationStatus.CONFIRMED&&minutes<=-20) alert(r,NotificationType.LATE_20,
                    "Khách trễ trên 20 phút","Lịch "+r.getCode()+" đã trễ trên 20 phút. Nhân viên cần quyết định giữ hoặc giải phóng bàn.");
        }
        deliverPending();
    }

    private void alert(Reservation r,NotificationType type,String title,String message){
        String suffix=r.getReservationDate()+"-"+r.effectiveTime();
        queue(r,type,NotificationChannel.IN_APP,"NHÂN VIÊN",title,message,"staff-"+type+"-"+r.getId()+"-"+suffix);
        String customer=message+" Nếu đang đến, vui lòng liên hệ nhà hàng và cung cấp mã "+r.getCode()+".";
        if(r.isNotifyEmail()&&r.getEmail()!=null)queue(r,type,NotificationChannel.EMAIL,r.getEmail(),title,customer,"email-"+type+"-"+r.getId()+"-"+suffix);
        if(r.isNotifySms())queue(r,type,NotificationChannel.SMS,r.getPhone(),title,customer,"sms-"+type+"-"+r.getId()+"-"+suffix);
    }

    private void queue(Reservation r,NotificationType type,NotificationChannel channel,String recipient,String title,String message,String key){
        if(!notifications.existsByDedupeKey(key))notifications.save(new OperationalNotification(r.getId(),type,channel,recipient,title,message,key));
    }

    public OperationalNotification createStaffAlert(Long reservationId,NotificationType type,String title,String message,String key){
        if(notifications.existsByDedupeKey(key))return null;
        return notifications.save(new OperationalNotification(reservationId,type,NotificationChannel.IN_APP,"NHÂN VIÊN",title,message,key));
    }

    public List<OperationalNotification> staffFeed(){return notifications.findTop100ByChannelOrderByCreatedAtDesc(NotificationChannel.IN_APP);}
    @Transactional public void markRead(Long id){notifications.findById(id).ifPresent(OperationalNotification::read);}

    private void deliverPending(){
        notifications.findAll().stream().filter(n->n.getStatus()==NotificationStatus.PENDING&&n.getChannel()!=NotificationChannel.IN_APP).forEach(n->{
            try{
                if(n.getChannel()==NotificationChannel.EMAIL){
                    if(!emailEnabled){n.sent(NotificationStatus.DEMO);log.info("[EMAIL DEMO] {} -> {}",n.getRecipient(),n.getMessage());return;}
                    SimpleMailMessage message=new SimpleMailMessage();message.setFrom(mailFrom);message.setTo(n.getRecipient());
                    message.setSubject(n.getTitle());message.setText(n.getMessage());mailSender.send(message);n.sent(NotificationStatus.SENT);
                }else{
                    log.info("[SMS {}] {} -> {}",smsSandbox?"SANDBOX":"PROVIDER",n.getRecipient(),n.getMessage());
                    n.sent(smsSandbox?NotificationStatus.DEMO:NotificationStatus.FAILED);
                }
            }catch(Exception ex){n.failed(ex.getMessage());}
        });
    }

    private String summary(Reservation r){
        return r.getReservationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))+" lúc "+r.effectiveTime()
                +", "+r.getPartySize()+" khách, thời lượng "+r.effectiveDurationMinutes()+" phút";
    }
}
