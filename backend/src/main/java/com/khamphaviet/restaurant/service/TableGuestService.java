package com.khamphaviet.restaurant.service;

import com.khamphaviet.restaurant.common.BusinessException;
import com.khamphaviet.restaurant.notification.*;
import com.khamphaviet.restaurant.reservation.*;
import com.khamphaviet.restaurant.table.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;

@Service
public class TableGuestService {
    private final RestaurantTableRepository tables;
    private final ReservationTableAssignmentRepository assignments;
    private final ReservationRepository reservations;
    private final ServiceSessionRepository sessions;
    private final TableServiceRequestRepository requests;
    private final NotificationService notifications;

    public TableGuestService(RestaurantTableRepository tables,ReservationTableAssignmentRepository assignments,
                             ReservationRepository reservations,ServiceSessionRepository sessions,
                             TableServiceRequestRepository requests,NotificationService notifications){
        this.tables=tables;this.assignments=assignments;this.reservations=reservations;this.sessions=sessions;
        this.requests=requests;this.notifications=notifications;
    }
    public record GuestTable(Long id,String code,String name,String area,int seats,boolean activeSession,List<TableServiceRequest> requests){}
    public record CreateRequest(TableRequestType type,String note){}

    public GuestTable view(String token){
        RestaurantTable table=findTable(token);ServiceSession session=activeSession(table.getId(),false);
        return new GuestTable(table.getId(),table.getCode(),table.getName(),table.getArea(),table.getSeats(),session!=null,
                requests.findByTableIdAndStatusInOrderByCreatedAtDesc(table.getId(),List.of(TableRequestStatus.NEW,TableRequestStatus.ACKNOWLEDGED)));
    }
    @Transactional
    public TableServiceRequest create(String token,CreateRequest request){
        RestaurantTable table=findTable(token);ServiceSession session=activeSession(table.getId(),true);
        if(request.type()==null)throw new BusinessException("Hãy chọn loại yêu cầu");
        if(requests.countByTableIdAndCreatedAtAfter(table.getId(),Instant.now().minusSeconds(30))>=2)
            throw new BusinessException("Bạn đã gửi yêu cầu. Vui lòng chờ nhân viên phản hồi");
        TableServiceRequest saved=requests.save(new TableServiceRequest(table.getId(),session.getId(),request.type(),request.note()));
        notifications.createStaffAlert(session.getReservationId(),NotificationType.TABLE_CALL,
                table.getCode()+" gọi nhân viên",label(request.type())+(request.note()==null?"":" – "+request.note()),"table-call-"+saved.getId());
        return saved;
    }
    public List<TableServiceRequest> staffFeed(){return requests.findTop100ByOrderByCreatedAtDesc();}
    @Transactional public TableServiceRequest update(Long id,TableRequestStatus status){
        TableServiceRequest request=requests.findById(id).orElseThrow(()->new BusinessException("Không tìm thấy yêu cầu"));
        request.change(status);return request;
    }
    private RestaurantTable findTable(String token){return tables.findByPublicToken(token).orElseThrow(()->new BusinessException("QR bàn không hợp lệ"));}
    private ServiceSession activeSession(Long tableId,boolean required){
        ServiceSession result=assignments.findByTableIdIn(List.of(tableId)).stream()
                .map(a->reservations.findById(a.getReservationId()).orElse(null)).filter(r->r!=null&&r.getStatus()==ReservationStatus.CHECKED_IN)
                .map(r->sessions.findByReservationId(r.getId()).orElse(null)).filter(s->s!=null&&s.getStatus()==ServiceSessionStatus.ACTIVE)
                .findFirst().orElse(null);
        if(required&&result==null)throw new BusinessException("Bàn chưa có phiên phục vụ đang hoạt động");
        return result;
    }
    private String label(TableRequestType type){return switch(type){case CALL_WAITER->"Khách gọi nhân viên";case WATER->"Khách xin thêm nước";case UTENSILS->"Khách xin thêm dụng cụ";case PAYMENT->"Khách yêu cầu thanh toán";};}
}
