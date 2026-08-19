package com.khamphaviet.restaurant.demo;

import com.khamphaviet.restaurant.common.BusinessException;
import com.khamphaviet.restaurant.deposit.*;
import com.khamphaviet.restaurant.menu.*;
import com.khamphaviet.restaurant.notification.*;
import com.khamphaviet.restaurant.order.*;
import com.khamphaviet.restaurant.reservation.*;
import com.khamphaviet.restaurant.service.*;
import com.khamphaviet.restaurant.table.*;
import com.khamphaviet.restaurant.timeout.*;
import com.khamphaviet.restaurant.walkin.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.*;
import java.util.List;

@Service
public class DemoScenarioService {
    private static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    private final boolean enabled;
    private final WalkInService walkIns;
    private final ReservationRepository reservations;
    private final ReservationTableAssignmentRepository assignments;
    private final ReservationService reservationService;
    private final ReservationDepositRepository deposits;
    private final ServiceSessionRepository sessions;
    private final RestaurantTableRepository tables;
    private final MenuItemRepository menu;
    private final DiningOrderService orderService;
    private final DiningOrderItemRepository orderItems;
    private final TableServiceRequestRepository tableRequests;
    private final NotificationService notifications;
    private final OperationalTimeoutService timeouts;
    private final OperationalTimePolicy policy;
    private final SecureRandom random=new SecureRandom();

    public DemoScenarioService(@Value("${app.demo-tools.enabled:false}") boolean enabled,
                               WalkInService walkIns, ReservationRepository reservations,
                               ReservationTableAssignmentRepository assignments, ReservationService reservationService,
                               ReservationDepositRepository deposits, ServiceSessionRepository sessions,
                               RestaurantTableRepository tables, MenuItemRepository menu,
                               DiningOrderService orderService, DiningOrderItemRepository orderItems,
                               TableServiceRequestRepository tableRequests, NotificationService notifications,
                               OperationalTimeoutService timeouts, OperationalTimePolicy policy) {
        this.enabled=enabled;this.walkIns=walkIns;this.reservations=reservations;this.assignments=assignments;
        this.reservationService=reservationService;this.deposits=deposits;this.sessions=sessions;this.tables=tables;
        this.menu=menu;this.orderService=orderService;this.orderItems=orderItems;this.tableRequests=tableRequests;
        this.notifications=notifications;this.timeouts=timeouts;this.policy=policy;
    }

    @Transactional
    public DemoScenarioDtos.CreateResponse create(DemoScenarioDtos.CreateRequest request,String actor) {
        if(!enabled)throw new BusinessException("Công cụ tạo tình huống chỉ bật trong môi trường demo");
        return switch(request.type()) {
            case WALK_IN_NORMAL,WALK_IN_WARNING,WALK_IN_CRITICAL->walkIn(request,actor);
            case RESERVATION_NEW,RESERVATION_UPCOMING,RESERVATION_LATE_WARNING,
                    RESERVATION_LATE_CRITICAL,RESERVATION_HOLD_EXPIRED,
                    RESERVATION_DEPOSIT_WAITING->reservation(request);
            case KITCHEN_NEW_ORDER,KITCHEN_PREPARING,KITCHEN_REPORTED_DELAY,
                    KITCHEN_OVERDUE_WARNING,KITCHEN_OVERDUE_CRITICAL,KITCHEN_READY->kitchen(request,actor);
            case QR_CALL_WAITER,QR_WATER,QR_PAYMENT,QR_UNANSWERED_WARNING,
                    QR_UNANSWERED_CRITICAL->tableRequest(request,actor);
            case TABLE_CLEANING_NORMAL,TABLE_CLEANING_WARNING,TABLE_CLEANING_CRITICAL->cleaning(request);
        };
    }

    private DemoScenarioDtos.CreateResponse walkIn(DemoScenarioDtos.CreateRequest request,String actor) {
        WalkInSlaLevel level=switch(request.type()) {
            case WALK_IN_NORMAL->WalkInSlaLevel.NORMAL;
            case WALK_IN_WARNING->WalkInSlaLevel.WARNING;
            default->WalkInSlaLevel.CRITICAL;
        };
        WalkInDtos.VisitResponse visit=walkIns.createDemoScenario(new WalkInDtos.DemoScenarioRequest(
                customer(request),phone(request),partySize(request),request.areaPreference(),
                request.priority()==null?WalkInPriority.NORMAL:request.priority(),request.priorityReason(),
                minutes(request,15),level,note(request)),actor);
        return result(request.type(),"Khách tại quán","Đã tạo khách "+label(level),
                visit.code()+" · "+visit.customerName()+" · "+visit.elapsedMinutes()+" phút",
                "/staff/walk-in",visit.id(),visit.reservationId(),visit.tableId());
    }

    private DemoScenarioDtos.CreateResponse reservation(DemoScenarioDtos.CreateRequest request) {
        ZonedDateTime now=ZonedDateTime.now(ZONE).withSecond(0).withNano(0);
        ZonedDateTime arrival=switch(request.type()) {
            case RESERVATION_UPCOMING->now.plusMinutes(policy.getUpcomingAlertMinutes());
            case RESERVATION_LATE_WARNING->now.minusMinutes(Math.max(policy.getLateWarningMinutes()+1,minutes(request,16)));
            case RESERVATION_LATE_CRITICAL->now.minusMinutes(Math.max(policy.getLateCriticalMinutes()+1,minutes(request,21)));
            default->now.plusHours(1);
        };
        Reservation reservation=createReservation(request,arrival);
        if(request.type()==DemoScenarioType.RESERVATION_NEW) {
            notifications.reservationCreated(reservation);
            return reservationResult(request,"Lịch đặt mới","Đã tạo đơn "+reservation.getCode()+" đang chờ đặt cọc",reservation);
        }
        if(request.type()==DemoScenarioType.RESERVATION_HOLD_EXPIRED) {
            reservation.simulateExpiredHold(Instant.now().minusSeconds((long)minutes(request,2)*60));
            reservations.saveAndFlush(reservation);timeouts.monitor();
            return reservationResult(request,"Hết hạn giữ chỗ",reservation.getCode()+" đã hết thời gian đặt cọc",reservation);
        }
        if(request.type()==DemoScenarioType.RESERVATION_DEPOSIT_WAITING) {
            ReservationDeposit deposit=new ReservationDeposit(reservation.getId(),BigDecimal.valueOf(200_000));
            deposit.pay(DepositMethod.PAYPAL,"DEMO-ORDER-"+reservation.getId(),"DEMO-CAPTURE-"+reservation.getId());
            deposit.simulatePaidAt(Instant.now().minusSeconds((long)(policy.getReservationConfirmationMinutes()+minutes(request,2))*60));
            deposits.saveAndFlush(deposit);notifications.depositPaid(reservation,"PAYPAL SANDBOX");timeouts.monitor();
            return reservationResult(request,"Cọc chờ xác nhận",reservation.getCode()+" đã nhận cọc nhưng nhân viên chưa xác nhận",reservation);
        }
        reservation.changeStatus(ReservationStatus.CONFIRMED);
        reservations.saveAndFlush(reservation);
        assignReservationTableIfRequested(reservation,request.tableId());
        notifications.reservationConfirmed(reservation);
        notifications.scheduleReminders();
        if(request.type()==DemoScenarioType.RESERVATION_LATE_WARNING||request.type()==DemoScenarioType.RESERVATION_LATE_CRITICAL)
            timeouts.monitor();
        String title=switch(request.type()) {
            case RESERVATION_UPCOMING->"Khách sắp đến";
            case RESERVATION_LATE_WARNING->"Khách trễ mức cảnh báo";
            default->"Khách trễ nghiêm trọng";
        };
        return reservationResult(request,title,reservation.getCode()+" · hẹn "+reservation.effectiveTime(),reservation);
    }

    private DemoScenarioDtos.CreateResponse kitchen(DemoScenarioDtos.CreateRequest request,String actor) {
        ServiceContext context=serviceContext(request,actor);
        MenuItem dish=request.menuItemId()==null
                ?menu.findByAvailableTrueOrderByFeaturedDescNameAsc().stream().findFirst()
                    .orElseThrow(()->new BusinessException("Chưa có món đang phục vụ để tạo demo"))
                :menu.findById(request.menuItemId()).filter(MenuItem::isAvailable)
                    .orElseThrow(()->new BusinessException("Món đã chọn không còn phục vụ"));
        DiningOrderDtos.OrderResponse order=orderService.create(context.session().getId(),
                new DiningOrderDtos.CreateRequest(List.of(new DiningOrderDtos.ItemRequest(dish.getId(),1)),
                        "[DEMO] "+note(request)));
        Long itemId=order.items().get(0).id();
        String title;
        String detail;
        switch(request.type()) {
            case KITCHEN_NEW_ORDER->{title="Món mới gọi";detail="Đang chờ bếp tiếp nhận";}
            case KITCHEN_PREPARING->{
                orderService.itemStatus(itemId,new DiningOrderDtos.ItemStatusRequest(DiningOrderItemStatus.PREPARING,null,null));
                title="Món đang chế biến";detail="Bếp đã bắt đầu nấu";
            }
            case KITCHEN_REPORTED_DELAY->{
                orderService.itemStatus(itemId,new DiningOrderDtos.ItemStatusRequest(DiningOrderItemStatus.PREPARING,null,null));
                int delayed=minutes(request,8);
                orderService.itemStatus(itemId,new DiningOrderDtos.ItemStatusRequest(DiningOrderItemStatus.DELAYED,delayed,reason(request,"Bếp đang đông")));
                title="Bếp báo món chậm";detail="ETA được cộng thêm "+delayed+" phút";
            }
            case KITCHEN_OVERDUE_WARNING,KITCHEN_OVERDUE_CRITICAL->{
                orderService.itemStatus(itemId,new DiningOrderDtos.ItemStatusRequest(DiningOrderItemStatus.PREPARING,null,null));
                int minimum=request.type()==DemoScenarioType.KITCHEN_OVERDUE_CRITICAL
                        ?policy.getKitchenCriticalOverdueMinutes()+1:1;
                int overdue=Math.max(minimum,minutes(request,request.type()==DemoScenarioType.KITCHEN_OVERDUE_CRITICAL?12:4));
                DiningOrderItem item=orderItems.findById(itemId).orElseThrow();
                item.simulateOverdueBy(overdue,reason(request,"Quá ETA tự động"));
                orderItems.saveAndFlush(item);timeouts.monitor();
                title=request.type()==DemoScenarioType.KITCHEN_OVERDUE_CRITICAL?"Món chậm nghiêm trọng":"Món tự động quá ETA";
                detail="Món đã chậm "+overdue+" phút";
            }
            case KITCHEN_READY->{
                orderService.itemStatus(itemId,new DiningOrderDtos.ItemStatusRequest(DiningOrderItemStatus.PREPARING,null,null));
                orderService.itemStatus(itemId,new DiningOrderDtos.ItemStatusRequest(DiningOrderItemStatus.READY,null,null));
                title="Món đã xong";detail="Nhân viên có thể mang ra bàn";
            }
            default->throw new IllegalStateException();
        }
        return result(request.type(),"Bếp & món ăn",title,
                context.table().getCode()+" · "+dish.getName()+" · "+detail,
                "/bep",itemId,context.reservation().getId(),context.table().getId());
    }

    private DemoScenarioDtos.CreateResponse tableRequest(DemoScenarioDtos.CreateRequest request,String actor) {
        ServiceContext context=serviceContext(request,actor);
        TableRequestType type=switch(request.type()) {
            case QR_WATER->TableRequestType.WATER;
            case QR_PAYMENT->TableRequestType.PAYMENT;
            default->TableRequestType.CALL_WAITER;
        };
        TableServiceRequest item=new TableServiceRequest(context.table().getId(),context.session().getId(),type,note(request));
        boolean critical=request.type()==DemoScenarioType.QR_UNANSWERED_CRITICAL;
        boolean overdue=critical||request.type()==DemoScenarioType.QR_UNANSWERED_WARNING;
        if(overdue) {
            int minutes=Math.max(policy.getTableRequestAckMinutes()+(critical?policy.getTableRequestAckMinutes()+1:1),
                    minutes(request,critical?8:4));
            item.simulateCreatedAt(Instant.now().minusSeconds((long)minutes*60));
        }
        tableRequests.saveAndFlush(item);
        notifications.createStaffAlert(context.reservation().getId(),NotificationType.TABLE_CALL,
                context.table().getCode()+" gọi nhân viên",label(type),"demo-table-call-"+item.getId());
        if(overdue)timeouts.monitor();
        String title=overdue?(critical?"QR chưa nhận nghiêm trọng":"QR chưa được tiếp nhận"):label(type);
        return result(request.type(),"QR gọi phục vụ",title,
                context.table().getCode()+" · "+label(type),"/staff/phuc-vu",item.getId(),
                context.reservation().getId(),context.table().getId());
    }

    private DemoScenarioDtos.CreateResponse cleaning(DemoScenarioDtos.CreateRequest request) {
        RestaurantTable table=availableTable(request.tableId(),1);
        int elapsed=switch(request.type()) {
            case TABLE_CLEANING_WARNING->policy.getCleaningTargetMinutes()+1;
            case TABLE_CLEANING_CRITICAL->policy.getCleaningTargetMinutes()*2+1;
            default->0;
        };
        if(request.minutes()!=null&&request.type()!=DemoScenarioType.TABLE_CLEANING_NORMAL)
            elapsed=Math.max(elapsed,request.minutes());
        table.simulateStatusSince(TableStatus.NEEDS_CLEANING,Instant.now().minusSeconds((long)elapsed*60));
        tables.saveAndFlush(table);
        if(elapsed>policy.getCleaningTargetMinutes())timeouts.monitor();
        String title=switch(request.type()) {
            case TABLE_CLEANING_NORMAL->"Bàn đang chờ dọn";
            case TABLE_CLEANING_WARNING->"Dọn bàn chậm";
            default->"Dọn bàn quá hạn nghiêm trọng";
        };
        return result(request.type(),"Trạng thái bàn",title,
                table.getCode()+" · đã chờ "+elapsed+" phút","/staff/ban",table.getId(),null,table.getId());
    }

    private Reservation createReservation(DemoScenarioDtos.CreateRequest request,ZonedDateTime arrival) {
        return reservations.save(new Reservation(nextCode(),customer(request),phone(request),blankToNull(request.email()),
                arrival.toLocalDate(),arrival.getHour()<15?"LUNCH":"DINNER",arrival.toLocalTime(),120,
                partySize(request),"GROUND_FLOOR","[DEMO] "+note(request),
                Boolean.TRUE.equals(request.notifyEmail()),false,policy.getReservationHoldMinutes()));
    }

    private void assignReservationTableIfRequested(Reservation reservation,Long tableId) {
        if(tableId==null)return;
        RestaurantTable table=availableTable(tableId,reservation.getPartySize());
        assignments.save(new ReservationTableAssignment(reservation.getId(),table.getId()));
        table.changeStatus(TableStatus.RESERVED);tables.save(table);
    }

    private ServiceContext serviceContext(DemoScenarioDtos.CreateRequest request,String actor) {
        ServiceContext existing=findActiveContext(request.tableId());
        if(existing!=null)return existing;
        RestaurantTable table=availableTable(request.tableId(),partySize(request));
        Reservation reservation=new Reservation(nextCode(),customer(request),phone(request),blankToNull(request.email()),
                LocalDate.now(ZONE),LocalTime.now(ZONE).isBefore(LocalTime.of(15,0))?"LUNCH":"DINNER",
                LocalTime.now(ZONE).withSecond(0).withNano(0),120,partySize(request),"GROUND_FLOOR",
                "[DEMO] Phiên phục vụ tự tạo",false,false,policy.getReservationHoldMinutes());
        reservation.markWalkIn();reservation.changeStatus(ReservationStatus.CONFIRMED);reservations.save(reservation);
        assignments.save(new ReservationTableAssignment(reservation.getId(),table.getId()));
        table.changeStatus(TableStatus.RESERVED);tables.save(table);
        reservationService.checkIn(reservation.getId(),actor);
        ServiceSession session=sessions.findByReservationId(reservation.getId()).orElseThrow();
        return new ServiceContext(table,reservation,session);
    }

    private ServiceContext findActiveContext(Long requestedTableId) {
        for(ServiceSession session:sessions.findByStatus(ServiceSessionStatus.ACTIVE)) {
            Reservation reservation=reservations.findById(session.getReservationId()).orElse(null);
            if(reservation==null)continue;
            for(ReservationTableAssignment assignment:assignments.findByReservationId(reservation.getId())) {
                if(requestedTableId!=null&&!requestedTableId.equals(assignment.getTableId()))continue;
                RestaurantTable table=tables.findById(assignment.getTableId()).orElse(null);
                if(table!=null&&table.getStatus()==TableStatus.OCCUPIED)return new ServiceContext(table,reservation,session);
            }
        }
        return null;
    }

    private RestaurantTable availableTable(Long requestedTableId,int partySize) {
        if(requestedTableId!=null) {
            RestaurantTable table=tables.findByIdForUpdate(requestedTableId)
                    .orElseThrow(()->new BusinessException("Không tìm thấy bàn đã chọn"));
            if(table.getStatus()!=TableStatus.AVAILABLE)throw new BusinessException("Bàn "+table.getCode()+" không còn trống; hãy chọn bàn khác hoặc để hệ thống tự chọn");
            if(table.getSeats()<partySize)throw new BusinessException("Bàn "+table.getCode()+" không đủ chỗ cho "+partySize+" khách");
            return table;
        }
        return tables.findByStatus(TableStatus.AVAILABLE).stream().filter(RestaurantTable::isActive)
                .filter(table->table.getSeats()>=partySize).sorted((a,b)->{
                    int seats=Integer.compare(a.getSeats(),b.getSeats());return seats!=0?seats:a.getCode().compareTo(b.getCode());
                }).findFirst().orElseThrow(()->new BusinessException("Không còn bàn trống phù hợp. Hãy hoàn tất hoặc dọn một bàn demo trước"));
    }

    private DemoScenarioDtos.CreateResponse reservationResult(DemoScenarioDtos.CreateRequest request,String title,
                                                                String message,Reservation reservation) {
        return result(request.type(),"Đặt bàn online",title,message,"/staff",reservation.getId(),reservation.getId(),request.tableId());
    }
    private DemoScenarioDtos.CreateResponse result(DemoScenarioType type,String group,String title,String message,
                                                     String path,Long entityId,Long reservationId,Long tableId) {
        return new DemoScenarioDtos.CreateResponse(type,group,title,message,path,entityId,reservationId,tableId);
    }
    private String customer(DemoScenarioDtos.CreateRequest request) {
        String name=blankToNull(request.customerName());
        if(name==null)name="Khách demo";
        return name.startsWith("[DEMO]")?name:"[DEMO] "+name;
    }
    private String phone(DemoScenarioDtos.CreateRequest request) {
        String phone=blankToNull(request.phone());return phone==null?"0901000001":phone;
    }
    private int partySize(DemoScenarioDtos.CreateRequest request) {return request.partySize()==null?2:request.partySize();}
    private int minutes(DemoScenarioDtos.CreateRequest request,int fallback) {return request.minutes()==null?fallback:request.minutes();}
    private String note(DemoScenarioDtos.CreateRequest request) {String value=blankToNull(request.note());return value==null?"Tình huống phục vụ kiểm thử":value;}
    private String reason(DemoScenarioDtos.CreateRequest request,String fallback) {String value=blankToNull(request.reason());return value==null?fallback:value;}
    private String blankToNull(String value) {return value==null||value.isBlank()?null:value.trim();}
    private String nextCode() {return "DM-"+LocalDate.now(ZONE).toString().replace("-","")+"-"+String.format("%04d",random.nextInt(10000));}
    private String label(WalkInSlaLevel level) {return switch(level){case NORMAL->"bình thường";case WARNING->"sắp trễ";case CRITICAL->"quá hạn";};}
    private String label(TableRequestType type) {return switch(type){case CALL_WAITER->"Khách gọi nhân viên";case WATER->"Khách xin thêm nước";case UTENSILS->"Khách xin dụng cụ";case PAYMENT->"Khách yêu cầu thanh toán";};}
    private record ServiceContext(RestaurantTable table,Reservation reservation,ServiceSession session) {}
}
