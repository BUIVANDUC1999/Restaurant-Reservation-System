package com.khamphaviet.restaurant.notification;

import com.khamphaviet.restaurant.reservation.*;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.khamphaviet.restaurant.timeout.OperationalTimePolicy;
import com.khamphaviet.restaurant.operations.StaffEventService;

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
    private final OperationalTimePolicy timePolicy;
    private final StaffEventService staffEvents;

    public NotificationService(OperationalNotificationRepository notifications, ReservationRepository reservations,
                               JavaMailSender mailSender,
                               @Value("${app.notifications.email-enabled:false}") boolean emailEnabled,
                               @Value("${app.notifications.sms-sandbox:true}") boolean smsSandbox,
                               @Value("${spring.mail.username:}") String mailUsername,
                               @Value("${spring.mail.password:}") String mailPassword,
                               OperationalTimePolicy timePolicy, StaffEventService staffEvents) {
        this.notifications=notifications;this.reservations=reservations;this.mailSender=mailSender;
        boolean credentialsPresent = mailUsername != null && !mailUsername.isBlank()
                && mailPassword != null && !mailPassword.isBlank();
        this.emailEnabled=emailEnabled&&credentialsPresent;this.smsSandbox=smsSandbox;this.mailFrom=mailUsername;
        this.timePolicy=timePolicy;
        this.staffEvents=staffEvents;
        if (emailEnabled && !credentialsPresent) {
            log.warn("Gmail đã được bật nhưng MAIL_USERNAME hoặc MAIL_PASSWORD còn thiếu; email tạm lưu ở chế độ DEMO");
        } else if (this.emailEnabled) {
            log.info("Gmail SMTP đã sẵn sàng với tài khoản {}", maskEmail(mailUsername));
        }
    }

    @Transactional
    public void reservationCreated(Reservation r) {
        queue(r,NotificationType.NEW_RESERVATION,NotificationChannel.IN_APP,"NHÂN VIÊN",
                "Có lịch đặt bàn mới",summary(r),"staff-new-"+r.getId());
        String customer="Kính gửi "+r.getCustomerName()+", nhà hàng đã nhận yêu cầu "+r.getCode()+". "+summary(r)
                +". Bàn được giữ "+timePolicy.getReservationHoldMinutes()+" phút để hoàn tất đặt cọc.";
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

    @Transactional
    public void reservationConfirmed(Reservation r) {
        String message="Đơn "+r.getCode()+" đã được nhà hàng xác nhận. "+summary(r)
                +". Vui lòng đến trước giờ hẹn khoảng 10 phút và cung cấp mã đặt bàn khi check-in.";
        queue(r,NotificationType.RESERVATION_CONFIRMED,NotificationChannel.IN_APP,"NHÂN VIÊN",
                "Đã xác nhận đơn đặt bàn",message,"staff-confirmed-"+r.getId());
        if(r.isNotifyEmail()&&r.getEmail()!=null) queue(r,NotificationType.RESERVATION_CONFIRMED,
                NotificationChannel.EMAIL,r.getEmail(),"Nhà hàng đã xác nhận đơn "+r.getCode(),
                message,"email-confirmed-"+r.getId());
        if(r.isNotifySms()) queue(r,NotificationType.RESERVATION_CONFIRMED,NotificationChannel.SMS,r.getPhone(),
                "Đơn đặt bàn đã được xác nhận",message,"sms-confirmed-"+r.getId());
        deliverPending();
    }

    @Scheduled(fixedDelayString="${app.timeouts.monitor-delay-ms:60000}")
    @Transactional
    public void scheduleReminders() {
        LocalDate today=LocalDate.now(ZONE); LocalDateTime now=LocalDateTime.now(ZONE);
        List<Reservation> active=reservations.findByReservationDateAndStatusIn(today,
                List.of(ReservationStatus.PENDING,ReservationStatus.CONFIRMED)).stream()
                .filter(r->r.getSource()!=ReservationSource.WALK_IN).toList();
        for(Reservation r:active){
            LocalDateTime arrival=LocalDateTime.of(r.getReservationDate(),r.effectiveTime());
            long minutes=Duration.between(now,arrival).toMinutes();
            int upcoming=timePolicy.getUpcomingAlertMinutes();
            int warning=timePolicy.getLateWarningMinutes();
            int critical=timePolicy.getLateCriticalMinutes();
            if(minutes>=upcoming-1&&minutes<=upcoming) alert(r,NotificationType.UPCOMING_30,"Khách sắp đến",
                    "Lịch "+r.getCode()+" sẽ đến sau khoảng "+upcoming+" phút.");
            if(r.getStatus()==ReservationStatus.CONFIRMED&&minutes<=-warning&&minutes>-critical) alert(r,NotificationType.LATE_15,
                    "Khách trễ "+warning+" phút","Lịch "+r.getCode()+" đã trễ "+warning+" phút. Vui lòng xác nhận khách đang đến.");
            if(r.getStatus()==ReservationStatus.CONFIRMED&&minutes<=-critical) alert(r,NotificationType.LATE_20,
                    "Khách trễ trên "+critical+" phút","Lịch "+r.getCode()+" đã trễ trên "+critical+" phút. Nhân viên cần quyết định giữ hoặc giải phóng bàn.");
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
        if(!notifications.existsByDedupeKey(key)) {
            notifications.save(new OperationalNotification(r.getId(),type,channel,recipient,title,message,key));
            if(channel==NotificationChannel.IN_APP) staffEvents.publish(type.name(),title,message,r.getId());
        }
    }

    public OperationalNotification createStaffAlert(Long reservationId,NotificationType type,String title,String message,String key){
        if(notifications.existsByDedupeKey(key))return null;
        OperationalNotification saved=notifications.save(new OperationalNotification(reservationId,type,NotificationChannel.IN_APP,"NHÂN VIÊN",title,message,key));
        staffEvents.publish(type.name(),title,message,reservationId);
        return saved;
    }

    public List<OperationalNotification> staffFeed(){return notifications.findTop100ByChannelOrderByCreatedAtDesc(NotificationChannel.IN_APP);}
    @Transactional public void markRead(Long id){notifications.findById(id).ifPresent(OperationalNotification::read);}

    private void deliverPending(){
        notifications.findAll().stream().filter(n->n.getStatus()==NotificationStatus.PENDING&&n.getChannel()!=NotificationChannel.IN_APP).forEach(n->{
            try{
                if(n.getChannel()==NotificationChannel.EMAIL){
                    if(!emailEnabled){n.sent(NotificationStatus.DEMO);log.info("[EMAIL DEMO] {} -> {}",n.getRecipient(),n.getMessage());return;}
                    MimeMessage message=mailSender.createMimeMessage();
                    MimeMessageHelper helper=new MimeMessageHelper(message,true,StandardCharsets.UTF_8.name());
                    helper.setFrom(mailFrom,"Khám Phá Việt");helper.setTo(n.getRecipient());helper.setSubject(n.getTitle());
                    helper.setText(n.getMessage(),buildEmailHtml(n));mailSender.send(message);n.sent(NotificationStatus.SENT);
                    log.info("[EMAIL SENT] {} - {}", maskEmail(n.getRecipient()), n.getTitle());
                }else{
                    log.info("[SMS {}] {} -> {}",smsSandbox?"SANDBOX":"PROVIDER",n.getRecipient(),n.getMessage());
                    n.sent(smsSandbox?NotificationStatus.DEMO:NotificationStatus.FAILED);
                }
            }catch(Exception ex){
                n.failed(ex.getMessage());
                log.warn("[EMAIL FAILED] {} - {}", maskEmail(n.getRecipient()), ex.getMessage());
            }
        });
    }

    private String summary(Reservation r){
        return r.getReservationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))+" lúc "+r.effectiveTime()
                +", "+r.getPartySize()+" khách, thời lượng "+r.effectiveDurationMinutes()+" phút";
    }

    private String buildEmailHtml(OperationalNotification notification) {
        Reservation reservation=notification.getReservationId()==null?null:
                reservations.findById(notification.getReservationId()).orElse(null);
        String badge=switch(notification.getType()){
            case NEW_RESERVATION -> "ĐÃ NHẬN YÊU CẦU";
            case DEPOSIT_PAID -> "ĐÃ NHẬN ĐẶT CỌC";
            case RESERVATION_CONFIRMED -> "ĐẶT BÀN ĐÃ XÁC NHẬN";
            case UPCOMING_30 -> "SẮP ĐẾN GIỜ HẸN";
            case LATE_15, LATE_20 -> "CẦN XÁC NHẬN GIỜ ĐẾN";
            default -> "THÔNG BÁO TỪ NHÀ HÀNG";
        };
        String guidance=switch(notification.getType()){
            case NEW_RESERVATION -> "Vui lòng hoàn tất đặt cọc trong thời gian giữ bàn. Sau khi nhận cọc, nhà hàng sẽ kiểm tra và xác nhận đơn.";
            case DEPOSIT_PAID -> "Khoản đặt cọc đã được ghi nhận. Nhân viên nhà hàng sẽ xác nhận lịch đặt bàn của bạn trong thời gian sớm nhất.";
            case RESERVATION_CONFIRMED -> "Bạn nên đến trước giờ hẹn khoảng 10 phút và cung cấp mã đặt bàn cho nhân viên lễ tân.";
            case UPCOMING_30 -> "Hãy kiểm tra thời gian di chuyển. Nếu cần thay đổi giờ đến, vui lòng liên hệ nhà hàng sớm.";
            case LATE_15, LATE_20 -> "Nếu bạn vẫn đang đến, vui lòng liên hệ nhà hàng và cung cấp mã đặt bàn để được tiếp tục giữ chỗ.";
            default -> "Vui lòng lưu lại mã đặt bàn để nhân viên có thể hỗ trợ bạn nhanh chóng.";
        };
        String details=reservation==null?"":reservationEmailDetails(reservation);
        return """
                <!doctype html>
                <html lang="vi">
                <body style="margin:0;background:#f4f0e8;font-family:Arial,Helvetica,sans-serif;color:#173a32">
                  <div style="display:none;max-height:0;overflow:hidden;color:transparent">{{PREHEADER}}</div>
                  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f4f0e8;padding:28px 12px">
                    <tr><td align="center">
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:620px;background:#ffffff;border-radius:18px;overflow:hidden;box-shadow:0 8px 30px rgba(15,55,45,.10)">
                        <tr><td style="background:#103f35;padding:28px 34px;color:#ffffff">
                          <table role="presentation" width="100%"><tr>
                            <td style="width:54px"><div style="width:46px;height:46px;line-height:46px;text-align:center;border:1px solid #e7b654;border-radius:50%;font-family:Georgia,serif;font-size:20px;color:#f3c86c">KV</div></td>
                            <td><div style="font-size:20px;font-weight:700;letter-spacing:1.5px">KHÁM PHÁ VIỆT</div><div style="margin-top:4px;color:#d6e4df;font-size:12px;letter-spacing:2px">ẨM THỰC TÂY BẮC</div></td>
                          </tr></table>
                        </td></tr>
                        <tr><td style="padding:34px">
                          <div style="display:inline-block;background:#fff4d8;color:#9a6616;border-radius:999px;padding:7px 12px;font-size:11px;font-weight:700;letter-spacing:.8px">{{BADGE}}</div>
                          <h1 style="margin:18px 0 12px;font-family:Georgia,serif;font-size:28px;line-height:1.25;color:#103f35">{{TITLE}}</h1>
                          <p style="margin:0 0 24px;color:#4c625d;font-size:15px;line-height:1.75">{{MESSAGE}}</p>
                          {{DETAILS}}
                          <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-top:24px;background:#eef6f2;border-left:4px solid #1b735e;border-radius:8px">
                            <tr><td style="padding:18px 20px"><div style="font-size:12px;font-weight:700;color:#1b735e;letter-spacing:.7px">VIỆC CẦN LÀM TIẾP THEO</div><div style="margin-top:7px;color:#34564d;font-size:14px;line-height:1.65">{{GUIDANCE}}</div></td></tr>
                          </table>
                          <p style="margin:24px 0 0;color:#74847f;font-size:12px;line-height:1.6">Bạn có thể tra cứu đơn trên website bằng mã đặt bàn và số điện thoại đã đăng ký.</p>
                        </td></tr>
                        <tr><td style="background:#f8f6f1;padding:20px 34px;border-top:1px solid #eee7da;color:#7a8581;font-size:11px;line-height:1.6">
                          Email được gửi tự động từ hệ thống Khám Phá Việt. Vui lòng không cung cấp mã đặt bàn cho người không liên quan.<br>Hotline 0900 000 000 · Mộc Châu, Sơn La · Hẹn gặp bạn tại nhà hàng.
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """
                .replace("{{PREHEADER}}",escapeHtml(notification.getTitle()))
                .replace("{{BADGE}}",escapeHtml(badge))
                .replace("{{TITLE}}",escapeHtml(notification.getTitle()))
                .replace("{{MESSAGE}}",escapeHtml(notification.getMessage()).replace("\n","<br>"))
                .replace("{{DETAILS}}",details)
                .replace("{{GUIDANCE}}",escapeHtml(guidance));
    }

    private String reservationEmailDetails(Reservation reservation) {
        String date=reservation.getReservationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String time=reservation.effectiveTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        return """
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border:1px solid #e8e2d6;border-radius:10px;border-collapse:separate;overflow:hidden">
                  <tr><td colspan="2" style="padding:16px 18px;background:#faf8f3;border-bottom:1px solid #e8e2d6;color:#6d7773;font-size:11px;font-weight:700;letter-spacing:.7px">CHI TIẾT ĐẶT BÀN</td></tr>
                  <tr><td style="padding:14px 18px;color:#73807c;font-size:13px;border-bottom:1px solid #eee9df">Mã đặt bàn</td><td align="right" style="padding:14px 18px;color:#b2781c;font-size:18px;font-weight:700;letter-spacing:1px;border-bottom:1px solid #eee9df">{{CODE}}</td></tr>
                  <tr><td style="padding:12px 18px;color:#73807c;font-size:13px;border-bottom:1px solid #eee9df">Ngày và giờ</td><td align="right" style="padding:12px 18px;color:#173a32;font-size:14px;font-weight:700;border-bottom:1px solid #eee9df">{{DATE}} · {{TIME}}</td></tr>
                  <tr><td style="padding:12px 18px;color:#73807c;font-size:13px;border-bottom:1px solid #eee9df">Số khách</td><td align="right" style="padding:12px 18px;color:#173a32;font-size:14px;font-weight:700;border-bottom:1px solid #eee9df">{{GUESTS}} khách</td></tr>
                  <tr><td style="padding:12px 18px;color:#73807c;font-size:13px">Thời lượng dự kiến</td><td align="right" style="padding:12px 18px;color:#173a32;font-size:14px;font-weight:700">{{DURATION}} phút</td></tr>
                </table>
                """
                .replace("{{CODE}}",escapeHtml(reservation.getCode()))
                .replace("{{DATE}}",escapeHtml(date))
                .replace("{{TIME}}",escapeHtml(time))
                .replace("{{GUESTS}}",String.valueOf(reservation.getPartySize()))
                .replace("{{DURATION}}",String.valueOf(reservation.effectiveDurationMinutes()));
    }

    private String escapeHtml(String value) {
        if(value==null)return "";
        return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }

    private String maskEmail(String email) {
        int separator = email == null ? -1 : email.indexOf('@');
        if (separator <= 1) return "***";
        return email.substring(0, 2) + "***" + email.substring(separator);
    }
}
