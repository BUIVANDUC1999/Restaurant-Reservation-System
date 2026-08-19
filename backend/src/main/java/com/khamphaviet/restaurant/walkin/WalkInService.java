package com.khamphaviet.restaurant.walkin;

import com.khamphaviet.restaurant.common.BusinessException;
import com.khamphaviet.restaurant.common.ConflictException;
import com.khamphaviet.restaurant.notification.*;
import com.khamphaviet.restaurant.order.DiningOrderRepository;
import com.khamphaviet.restaurant.reservation.*;
import com.khamphaviet.restaurant.service.*;
import com.khamphaviet.restaurant.table.*;
import com.khamphaviet.restaurant.timeout.OperationalTimePolicy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.security.SecureRandom;
import java.time.*;
import java.util.*;

@Service
public class WalkInService {
    private static final List<WalkInStatus> ACTIVE = List.of(WalkInStatus.WAITING, WalkInStatus.TABLE_OFFERED,
            WalkInStatus.SEATED, WalkInStatus.DINING, WalkInStatus.PAYMENT_REQUESTED, WalkInStatus.CLEANING);
    private static final List<ReservationStatus> ONLINE_BLOCKING = List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
    private final WalkInVisitRepository visits;
    private final WalkInEventRepository events;
    private final WalkInPolicy policy;
    private final OperationalTimePolicy timePolicy;
    private final RestaurantTableRepository tables;
    private final ReservationRepository reservations;
    private final ReservationTableAssignmentRepository assignments;
    private final ReservationService reservationService;
    private final ServiceSessionRepository sessions;
    private final DiningOrderRepository orders;
    private final NotificationService notifications;
    private final boolean demoToolsEnabled;
    private final SecureRandom random = new SecureRandom();

    public WalkInService(WalkInVisitRepository visits, WalkInEventRepository events, WalkInPolicy policy,
                         OperationalTimePolicy timePolicy, RestaurantTableRepository tables,
                         ReservationRepository reservations, ReservationTableAssignmentRepository assignments,
                         ReservationService reservationService, ServiceSessionRepository sessions,
                         DiningOrderRepository orders, NotificationService notifications,
                         @Value("${app.demo-tools.enabled:false}") boolean demoToolsEnabled) {
        this.visits=visits;this.events=events;this.policy=policy;this.timePolicy=timePolicy;this.tables=tables;
        this.reservations=reservations;this.assignments=assignments;this.reservationService=reservationService;
        this.sessions=sessions;this.orders=orders;this.notifications=notifications;
        this.demoToolsEnabled=demoToolsEnabled;
    }

    @Transactional
    public WalkInDtos.VisitResponse createDemoScenario(WalkInDtos.DemoScenarioRequest request,String actor) {
        if (!demoToolsEnabled) throw new BusinessException("Công cụ tạo tình huống chỉ bật trong môi trường demo");
        String name=request.customerName().trim();
        if(!name.startsWith("[DEMO]")) name="[DEMO] "+name;
        WalkInDtos.VisitResponse created=create(new WalkInDtos.CreateRequest(name,request.phone(),request.partySize(),
                request.areaPreference(),request.priority(),request.priorityReason(),request.quotedWaitMinutes(),
                request.note()),actor);
        WalkInVisit visit=find(created.id());
        long elapsedMinutes=switch(request.slaLevel()){
            case NORMAL->Math.max(0,Math.round(request.quotedWaitMinutes()*.4));
            case WARNING->Math.max(1,(long)Math.ceil(request.quotedWaitMinutes()*.85));
            case CRITICAL->request.quotedWaitMinutes()+policy.getWaitCriticalGraceMinutes()+1L;
        };
        visit.simulateWaitingSince(Instant.now().minusSeconds(elapsedMinutes*60));
        log(visit,WalkInStatus.WAITING,WalkInStatus.WAITING,"DEMO_SETUP",
                "Tạo thủ công tình huống "+request.slaLevel()+" · đã chờ giả lập "+elapsedMinutes+" phút",actor);
        if(request.slaLevel()!=WalkInSlaLevel.NORMAL)
            notifications.createStaffAlert(null,NotificationType.WALK_IN_TIMEOUT,
                    request.slaLevel()==WalkInSlaLevel.CRITICAL?"Walk-in demo quá SLA":"Walk-in demo sắp quá SLA",
                    visit.getCode()+" · "+visit.getCustomerName(),"walk-in-demo-"+visit.getId());
        return response(visit,true);
    }

    @Transactional
    public WalkInDtos.VisitResponse create(WalkInDtos.CreateRequest request, String actor) {
        WalkInPriority priority=request.priority()==null?WalkInPriority.NORMAL:request.priority();
        if(priority!=WalkInPriority.NORMAL&&(request.priorityReason()==null||request.priorityReason().isBlank()))
            throw new BusinessException("Cần nhập lý do khi ưu tiên khách");
        List<WalkInDtos.SuggestedTable> suggestions=suggestions(request.partySize(),request.areaPreference());
        int quote=request.quotedWaitMinutes()==null?estimatedWait(suggestions):request.quotedWaitMinutes();
        WalkInVisit visit=visits.save(new WalkInVisit(nextCode(),request.customerName().trim(),request.phone(),
                request.partySize(),request.areaPreference(),priority,request.priorityReason(),quote,request.note()));
        log(visit,null,WalkInStatus.WAITING,"ARRIVED","Tiếp nhận khách tại quán",actor);
        notifications.createStaffAlert(null,NotificationType.WALK_IN,"Khách tại quán mới",
                visit.getCode()+" · "+visit.getPartySize()+" khách · dự kiến chờ "+quote+" phút","walk-in-new-"+visit.getId());
        return response(visit,true);
    }

    public List<WalkInDtos.VisitResponse> list() {
        return visits.findAllByOrderByArrivedAtDesc().stream().map(v->response(v,ACTIVE.contains(v.getStatus())))
                .sorted(Comparator.comparingInt((WalkInDtos.VisitResponse v)->ACTIVE.contains(v.status())?0:1)
                        .thenComparingInt(v->v.slaLevel()==WalkInSlaLevel.CRITICAL?0:v.slaLevel()==WalkInSlaLevel.WARNING?1:2)
                        .thenComparingInt(v->priorityWeight(v.priority()))
                        .thenComparing(WalkInDtos.VisitResponse::expectedSeatAt))
                .toList();
    }

    public WalkInDtos.MetricsResponse metrics(){
        List<WalkInVisit> all=visits.findAll();
        List<WalkInVisit> seated=all.stream().filter(v->v.getSeatedAt()!=null).toList();
        List<Long> waits=seated.stream().map(v->Duration.between(v.getArrivedAt(),v.getSeatedAt()).toMinutes()).sorted().toList();
        double average=waits.stream().mapToLong(Long::longValue).average().orElse(0);
        long p90=waits.isEmpty()?0:waits.get(Math.min(waits.size()-1,(int)Math.ceil(waits.size()*.9)-1));
        double quoteAccuracy=seated.isEmpty()?100:seated.stream().filter(v->
                Math.abs(Duration.between(v.getArrivedAt(),v.getSeatedAt()).toMinutes()-v.getQuotedWaitMinutes())<=5).count()*100.0/seated.size();
        long abandoned=all.stream().filter(v->List.of(WalkInStatus.LEFT,WalkInStatus.NO_RESPONSE,WalkInStatus.CANCELLED).contains(v.getStatus())).count();
        List<Long> cleaning=all.stream().filter(v->v.getCleaningStartedAt()!=null&&v.getCompletedAt()!=null)
                .map(v->Duration.between(v.getCleaningStartedAt(),v.getCompletedAt()).toMinutes()).toList();
        return new WalkInDtos.MetricsResponse(all.size(),all.stream().filter(v->ACTIVE.contains(v.getStatus())).count(),
                round(average),p90,round(quoteAccuracy),all.isEmpty()?0:round(abandoned*100.0/all.size()),
                round(cleaning.stream().mapToLong(Long::longValue).average().orElse(0)));
    }

    public WalkInDtos.VisitResponse get(Long id){return response(find(id),true);}

    @Transactional
    public WalkInDtos.VisitResponse reviseQuote(Long id,int minutes,String note,String actor){
        WalkInVisit visit=find(id);require(visit,WalkInStatus.WAITING);
        visit.reviseQuote(minutes);log(visit,WalkInStatus.WAITING,WalkInStatus.WAITING,"REVISE_QUOTE",note,actor);
        return response(visit,true);
    }

    @Transactional
    public WalkInDtos.VisitResponse offer(Long id,WalkInDtos.OfferRequest request,String actor){
        WalkInVisit visit=find(id);require(visit,WalkInStatus.WAITING);
        RestaurantTable table=tables.findByIdForUpdate(request.tableId()).orElseThrow(()->new BusinessException("Không tìm thấy bàn"));
        if(table.getStatus()!=TableStatus.AVAILABLE)
            throw new ConflictException("Bàn "+table.getCode()+" vừa được nhân viên khác giữ hoặc xếp khách");
        WalkInDtos.SuggestedTable suggestion=suggestions(visit.getPartySize(),visit.getAreaPreference()).stream()
                .filter(item->item.id().equals(table.getId())).findFirst()
                .orElseThrow(()->new BusinessException("Bàn không phù hợp hoặc đang được sử dụng"));
        if(!suggestion.safe()||suggestion.availableAt().isAfter(Instant.now().plusSeconds(30)))
            throw new ConflictException(suggestion.reason());
        Reservation backing=new Reservation(visit.getCode(),visit.getCustomerName(),
                visit.getPhone()==null||visit.getPhone().isBlank()?"0000000000":visit.getPhone(),null,
                LocalDate.now(),LocalTime.now().isBefore(LocalTime.of(15,0))?"LUNCH":"DINNER",
                LocalTime.now().withSecond(0).withNano(0),policy.getDiningMinutes(),visit.getPartySize(),
                "GROUND_FLOOR","Khách tại quán "+visit.getCode(),false,false,policy.getOfferExpiryMinutes());
        backing.markWalkIn();backing.changeStatus(ReservationStatus.CONFIRMED);reservations.save(backing);
        assignments.save(new ReservationTableAssignment(backing.getId(),table.getId()));
        table.changeStatus(TableStatus.RESERVED);
        WalkInStatus from=visit.getStatus();visit.offer(table.getId(),backing.getId(),policy.getOfferExpiryMinutes());
        log(visit,from,visit.getStatus(),"TABLE_OFFERED",
                "Mời khách vào "+table.getCode()+(request.note()==null?"":" · "+request.note()),actor);
        return response(visit,true);
    }

    @Transactional
    public WalkInDtos.VisitResponse callAgain(Long id,String note,String actor){
        WalkInVisit visit=find(id);require(visit,WalkInStatus.TABLE_OFFERED);
        visit.callAgain(policy.getOfferExpiryMinutes());
        log(visit,WalkInStatus.TABLE_OFFERED,WalkInStatus.TABLE_OFFERED,"CALL_AGAIN",note,actor);
        return response(visit,true);
    }

    @Transactional
    public WalkInDtos.VisitResponse seat(Long id,String note,String actor){
        WalkInVisit visit=find(id);require(visit,WalkInStatus.TABLE_OFFERED);
        reservationService.checkIn(visit.getReservationId(),actor);
        WalkInStatus from=visit.getStatus();visit.seat();
        log(visit,from,visit.getStatus(),"SEATED",note,actor);
        return response(visit,true);
    }

    @Transactional
    public WalkInDtos.VisitResponse dining(Long id,String note,String actor){
        WalkInVisit visit=find(id);
        if(!List.of(WalkInStatus.SEATED,WalkInStatus.DINING).contains(visit.getStatus()))
            throw new BusinessException("Khách chưa được xếp vào bàn");
        WalkInStatus from=visit.getStatus();visit.dining();log(visit,from,visit.getStatus(),"DINING",note,actor);
        return response(visit,true);
    }

    @Transactional
    public WalkInDtos.VisitResponse requestPayment(Long id,String note,String actor){
        WalkInVisit visit=find(id);
        if(!List.of(WalkInStatus.SEATED,WalkInStatus.DINING).contains(visit.getStatus()))
            throw new BusinessException("Lượt khách chưa ở trạng thái phục vụ");
        WalkInStatus from=visit.getStatus();visit.requestPayment();
        log(visit,from,visit.getStatus(),"PAYMENT_REQUESTED",note,actor);return response(visit,true);
    }

    @Transactional
    public WalkInDtos.VisitResponse finishService(Long id,String note,String actor){
        WalkInVisit visit=find(id);
        if(!List.of(WalkInStatus.SEATED,WalkInStatus.DINING,WalkInStatus.PAYMENT_REQUESTED).contains(visit.getStatus()))
            throw new BusinessException("Không thể hoàn tất lượt khách ở trạng thái hiện tại");
        reservationService.completeService(visit.getReservationId(),actor);
        WalkInStatus from=visit.getStatus();visit.cleaning();
        log(visit,from,visit.getStatus(),"CLEANING",note,actor);return response(visit,true);
    }

    @Transactional
    public WalkInDtos.VisitResponse cleaned(Long id,String note,String actor){
        WalkInVisit visit=find(id);require(visit,WalkInStatus.CLEANING);
        RestaurantTable table=tables.findById(visit.getTableId()).orElseThrow(()->new BusinessException("Không tìm thấy bàn"));
        table.changeStatus(TableStatus.AVAILABLE);
        WalkInStatus from=visit.getStatus();visit.complete();
        log(visit,from,visit.getStatus(),"COMPLETED",note==null?"Bàn đã được dọn và sẵn sàng":note,actor);
        return response(visit,false);
    }

    @Transactional
    public WalkInDtos.VisitResponse exit(Long id,WalkInStatus terminal,String note,String actor){
        if(!List.of(WalkInStatus.LEFT,WalkInStatus.NO_RESPONSE,WalkInStatus.CANCELLED).contains(terminal))
            throw new BusinessException("Trạng thái kết thúc không hợp lệ");
        WalkInVisit visit=find(id);
        if(!List.of(WalkInStatus.WAITING,WalkInStatus.TABLE_OFFERED).contains(visit.getStatus()))
            throw new BusinessException("Khách đã vào bàn, không thể rời hàng chờ");
        WalkInStatus from=visit.getStatus();
        releaseOffer(visit,terminal==WalkInStatus.NO_RESPONSE?ReservationStatus.NO_SHOW:ReservationStatus.CANCELLED);
        visit.exit(terminal);log(visit,from,terminal,terminal.name(),note,actor);return response(visit,false);
    }

    @Transactional
    public WalkInDtos.VisitResponse requeue(Long id,Integer quote,String note,String actor){
        WalkInVisit visit=find(id);
        if(!List.of(WalkInStatus.NO_RESPONSE,WalkInStatus.LEFT).contains(visit.getStatus()))
            throw new BusinessException("Chỉ có thể đưa khách bỏ lượt trở lại hàng chờ");
        WalkInStatus from=visit.getStatus();visit.requeue(quote==null?policy.getDefaultWaitMinutes():quote);
        log(visit,from,visit.getStatus(),"REQUEUE",note,actor);return response(visit,true);
    }

    @Scheduled(fixedDelayString="${app.timeouts.monitor-delay-ms:60000}",initialDelayString="20000")
    @Transactional
    public void monitor(){
        for(WalkInVisit visit:visits.findByStatusIn(ACTIVE)){
            if(visit.getStatus()==WalkInStatus.CLEANING&&visit.getTableId()!=null&&
                    tables.findById(visit.getTableId()).map(t->t.getStatus()==TableStatus.AVAILABLE).orElse(false)){
                WalkInStatus from=visit.getStatus();visit.complete();
                log(visit,from,visit.getStatus(),"COMPLETED","Bàn đã được dọn và sẵn sàng","SYSTEM");
                continue;
            }
            Sla sla=sla(visit);
            if(sla.level()!=WalkInSlaLevel.NORMAL){
                notifications.createStaffAlert(visit.getReservationId(),NotificationType.WALK_IN_TIMEOUT,
                        sla.level()==WalkInSlaLevel.CRITICAL?"Walk-in quá SLA":"Walk-in sắp quá SLA",
                        visit.getCode()+" · "+sla.message(),
                        "walk-in-sla-"+visit.getId()+"-"+visit.getStatus()+"-"+sla.level());
            }
        }
    }

    private void releaseOffer(WalkInVisit visit,ReservationStatus status){
        if(visit.getReservationId()!=null){
            Reservation reservation=reservations.findById(visit.getReservationId()).orElse(null);
            if(reservation!=null&&reservation.getStatus()==ReservationStatus.CONFIRMED)
                reservationService.updateStatus(reservation.getId(),status,
                        "Giải phóng bàn do lượt khách trực tiếp không tiếp tục","SYSTEM");
        }
    }

    private WalkInDtos.VisitResponse response(WalkInVisit visit,boolean includeSuggestions){
        Sla sla=sla(visit);
        String tableCode=visit.getTableId()==null?null:tables.findById(visit.getTableId()).map(RestaurantTable::getCode).orElse(null);
        Long sessionId=visit.getReservationId()==null?null:sessions.findByReservationId(visit.getReservationId()).map(ServiceSession::getId).orElse(null);
        List<WalkInDtos.EventResponse> timeline=events.findByWalkInVisitIdOrderByCreatedAtDesc(visit.getId()).stream()
                .map(e->new WalkInDtos.EventResponse(e.getId(),e.getFromStatus(),e.getToStatus(),e.getAction(),e.getNote(),e.getActor(),e.getCreatedAt())).toList();
        return new WalkInDtos.VisitResponse(visit.getId(),visit.getCode(),visit.getCustomerName(),visit.getPhone(),
                visit.getPartySize(),visit.getAreaPreference(),visit.getPriority(),visit.getPriorityReason(),visit.getStatus(),
                sla.level(),sla.message(),sla.deadline(),Duration.between(visit.getArrivedAt(),Instant.now()).toMinutes(),
                visit.getQuotedWaitMinutes(),visit.getExpectedSeatAt(),visit.getOfferedAt(),visit.getOfferExpiresAt(),
                visit.getSeatedAt(),visit.getPaymentRequestedAt(),visit.getCleaningStartedAt(),visit.getCompletedAt(),
                visit.getTableId(),tableCode,visit.getReservationId(),sessionId,visit.getCallCount(),visit.getNote(),
                includeSuggestions&&visit.getStatus()==WalkInStatus.WAITING?suggestions(visit.getPartySize(),visit.getAreaPreference()):List.of(),timeline);
    }

    private List<WalkInDtos.SuggestedTable> suggestions(int partySize,String areaPreference){
        Instant now=Instant.now();
        return tables.findAllByOrderByFloorAscCodeAsc().stream().filter(RestaurantTable::isActive)
                .filter(table->table.getSeats()>=partySize)
                .map(table->suggestion(table,now,partySize))
                .sorted(Comparator.comparingInt((WalkInDtos.SuggestedTable s)->s.safe()?0:1)
                        .thenComparingInt(s->matchesArea(s,areaPreference)?0:1)
                        .thenComparing(WalkInDtos.SuggestedTable::availableAt)
                        .thenComparingInt(WalkInDtos.SuggestedTable::seatWaste))
                .limit(6).toList();
    }

    private WalkInDtos.SuggestedTable suggestion(RestaurantTable table,Instant now,int partySize){
        Instant available=switch(table.getStatus()){
            case AVAILABLE->now;
            case NEEDS_CLEANING->table.getStatusChangedAt().plusSeconds(timePolicy.getCleaningTargetMinutes()*60L);
            case OCCUPIED->table.getStatusChangedAt().plusSeconds((long)policy.getDiningMinutes()*60);
            case RESERVED,INACTIVE->now.plusSeconds((long)policy.getDefaultWaitMinutes()*60);
        };
        if(available.isBefore(now))available=now;
        Instant next=nextOnlineReservation(table.getId());
        boolean statusSafe=table.getStatus()!=TableStatus.RESERVED&&table.getStatus()!=TableStatus.INACTIVE;
        boolean scheduleSafe=next==null||available.plusSeconds((long)(policy.getDiningMinutes()+timePolicy.getCleaningBufferMinutes())*60).isBefore(next);
        boolean safe=statusSafe&&scheduleSafe;
        String reason=!statusSafe?"Bàn đang được giữ hoặc tạm ngưng":!scheduleSafe?
                "Có lịch online sắp tới lúc "+LocalDateTime.ofInstant(next,ZoneId.systemDefault()).toLocalTime():
                available.equals(now)?"Có thể xếp ngay":"Dự kiến sẵn sàng sau "+Math.max(1,Duration.between(now,available).toMinutes())+" phút";
        return new WalkInDtos.SuggestedTable(table.getId(),table.getCode(),table.getName(),table.getArea(),table.getSeats(),
                table.getSeats()-partySize,available,next,safe,reason);
    }

    private Instant nextOnlineReservation(Long tableId){
        Set<Long> ids=assignments.findByTableIdIn(List.of(tableId)).stream().map(ReservationTableAssignment::getReservationId)
                .collect(java.util.stream.Collectors.toSet());
        Instant now=Instant.now();
        return reservations.findAllById(ids).stream()
                .filter(r->r.getSource()==ReservationSource.ONLINE&&ONLINE_BLOCKING.contains(r.getStatus()))
                .map(r->LocalDateTime.of(r.getReservationDate(),r.effectiveTime()).atZone(ZoneId.systemDefault()).toInstant())
                .filter(at->at.isAfter(now)).min(Instant::compareTo).orElse(null);
    }

    private boolean matchesArea(WalkInDtos.SuggestedTable suggestion,String area){
        return area==null||area.isBlank()||suggestion.area().toLowerCase().contains(area.toLowerCase());
    }
    private int estimatedWait(List<WalkInDtos.SuggestedTable> suggestions){
        return suggestions.stream().filter(WalkInDtos.SuggestedTable::safe)
                .mapToInt(s->(int)Math.max(0,Duration.between(Instant.now(),s.availableAt()).toMinutes()))
                .min().orElse(policy.getDefaultWaitMinutes());
    }

    private Sla sla(WalkInVisit visit){
        Instant now=Instant.now();Instant deadline;WalkInSlaLevel level=WalkInSlaLevel.NORMAL;String message;
        switch(visit.getStatus()){
            case WAITING->{
                deadline=visit.getExpectedSeatAt();
                Instant warning=visit.getArrivedAt().plusSeconds(Math.round(visit.getQuotedWaitMinutes()*60L*.8));
                if(now.isAfter(deadline.plusSeconds(policy.getWaitCriticalGraceMinutes()*60L)))level=WalkInSlaLevel.CRITICAL;
                else if(now.isAfter(warning))level=WalkInSlaLevel.WARNING;
                message=level==WalkInSlaLevel.NORMAL?"Đang chờ đúng ETA":level==WalkInSlaLevel.WARNING?
                        "Sắp đến thời gian đã hẹn":"Đã trễ thời gian xếp bàn";
            }
            case TABLE_OFFERED->{
                deadline=visit.getOfferExpiresAt();
                if(now.isAfter(deadline))level=WalkInSlaLevel.CRITICAL;
                else if(now.isAfter(visit.getOfferedAt().plusSeconds(policy.getOfferWarningMinutes()*60L)))level=WalkInSlaLevel.WARNING;
                message=level==WalkInSlaLevel.NORMAL?"Đang chờ khách vào bàn":level==WalkInSlaLevel.WARNING?
                        "Cần gọi khách lần nữa":"Khách chưa phản hồi sau "+policy.getOfferExpiryMinutes()+" phút";
            }
            case SEATED->{
                deadline=visit.getSeatedAt().plusSeconds(policy.getSeatedCriticalMinutes()*60L);
                boolean hasOrder=visit.getReservationId()!=null&&sessions.findByReservationId(visit.getReservationId())
                        .map(s->!orders.findByServiceSessionIdOrderByCreatedAtDesc(s.getId()).isEmpty()).orElse(false);
                if(!hasOrder&&now.isAfter(deadline))level=WalkInSlaLevel.CRITICAL;
                else if(!hasOrder&&now.isAfter(visit.getSeatedAt().plusSeconds(policy.getSeatedWarningMinutes()*60L)))level=WalkInSlaLevel.WARNING;
                message=hasOrder?"Đã có phiếu gọi món":level==WalkInSlaLevel.NORMAL?"Khách vừa ngồi bàn":
                        level==WalkInSlaLevel.WARNING?"Khách chưa gọi món":"Khách ngồi quá lâu chưa có phiếu món";
            }
            case PAYMENT_REQUESTED->{
                deadline=visit.getPaymentRequestedAt().plusSeconds(policy.getPaymentCriticalMinutes()*60L);
                if(now.isAfter(deadline))level=WalkInSlaLevel.CRITICAL;
                else if(now.isAfter(visit.getPaymentRequestedAt().plusSeconds(policy.getPaymentWarningMinutes()*60L)))level=WalkInSlaLevel.WARNING;
                message=level==WalkInSlaLevel.NORMAL?"Đang chờ thanh toán":level==WalkInSlaLevel.WARNING?
                        "Yêu cầu thanh toán sắp quá hạn":"Yêu cầu thanh toán chưa được xử lý";
            }
            case CLEANING->{
                deadline=visit.getCleaningStartedAt().plusSeconds(timePolicy.getCleaningTargetMinutes()*60L);
                if(now.isAfter(deadline))level=WalkInSlaLevel.CRITICAL;
                else if(now.isAfter(visit.getCleaningStartedAt().plusSeconds(10*60L)))level=WalkInSlaLevel.WARNING;
                message=level==WalkInSlaLevel.NORMAL?"Bàn đang được dọn":level==WalkInSlaLevel.WARNING?
                        "Bàn sắp quá thời gian dọn":"Bàn chưa được dọn đúng SLA";
            }
            default->{deadline=visit.getExpectedSeatAt();message="Đang phục vụ";}
        }
        return new Sla(level,message,deadline);
    }

    private void log(WalkInVisit visit,WalkInStatus from,WalkInStatus to,String action,String note,String actor){
        events.save(new WalkInEvent(visit.getId(),from,to,action,note,actor==null?"SYSTEM":actor));
    }
    private WalkInVisit find(Long id){return visits.findById(id).orElseThrow(()->new BusinessException("Không tìm thấy lượt khách tại quán"));}
    private int priorityWeight(WalkInPriority priority){return switch(priority){case MANAGER->0;case ACCESSIBILITY->1;case ELDERLY->2;case NORMAL->3;};}
    private double round(double value){return Math.round(value*10.0)/10.0;}
    private void require(WalkInVisit visit,WalkInStatus status){if(visit.getStatus()!=status)throw new BusinessException("Trạng thái lượt khách không hợp lệ");}
    private String nextCode(){return "WI-"+LocalDate.now().toString().replace("-","")+"-"+String.format("%04d",random.nextInt(10000));}
    private record Sla(WalkInSlaLevel level,String message,Instant deadline){}
}
