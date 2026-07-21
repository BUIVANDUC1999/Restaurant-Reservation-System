package com.khamphaviet.restaurant.table;

import com.khamphaviet.restaurant.order.*;
import com.khamphaviet.restaurant.reservation.*;
import com.khamphaviet.restaurant.service.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class TableOverviewService {
    private static final List<DiningOrderStatus> OPEN = List.of(DiningOrderStatus.SUBMITTED, DiningOrderStatus.PREPARING, DiningOrderStatus.READY);
    private final RestaurantTableRepository tables; private final ReservationTableAssignmentRepository assignments;
    private final ReservationRepository reservations; private final ServiceSessionRepository sessions; private final DiningOrderRepository orders;

    public TableOverviewService(RestaurantTableRepository tables, ReservationTableAssignmentRepository assignments,
                                ReservationRepository reservations, ServiceSessionRepository sessions, DiningOrderRepository orders) {
        this.tables=tables;this.assignments=assignments;this.reservations=reservations;this.sessions=sessions;this.orders=orders;
    }

    public record TableOverview(Long id,String code,String name,String floor,String area,Integer seats,TableStatus status,boolean active,
                                TableServiceState serviceState,Long reservationId,String reservationCode,String customerName,
                                String customerPhone,Integer partySize,Long serviceSessionId,long openOrderCount,long readyOrderCount) {}

    public List<TableOverview> list() {
        List<RestaurantTable> allTables=tables.findAllByOrderByFloorAscCodeAsc();
        List<ReservationTableAssignment> allAssignments=assignments.findByTableIdIn(allTables.stream().map(RestaurantTable::getId).toList());
        Map<Long,Reservation> reservationMap=new HashMap<>();
        reservations.findAllById(allAssignments.stream().map(ReservationTableAssignment::getReservationId).distinct().toList()).forEach(r->reservationMap.put(r.getId(),r));
        Map<Long,List<ReservationTableAssignment>> byTable=new HashMap<>();
        allAssignments.forEach(a->byTable.computeIfAbsent(a.getTableId(),key->new ArrayList<>()).add(a));
        return allTables.stream().map(table->overview(table,byTable.getOrDefault(table.getId(),List.of()),reservationMap)).toList();
    }

    private TableOverview overview(RestaurantTable table,List<ReservationTableAssignment> tableAssignments,Map<Long,Reservation> reservationMap) {
        Reservation reservation=tableAssignments.stream().map(a->reservationMap.get(a.getReservationId())).filter(Objects::nonNull)
                .filter(r->table.getStatus()==TableStatus.OCCUPIED?r.getStatus()==ReservationStatus.CHECKED_IN:
                        table.getStatus()==TableStatus.RESERVED?List.of(ReservationStatus.PENDING,ReservationStatus.CONFIRMED).contains(r.getStatus()):false)
                .max(Comparator.comparing(Reservation::getCreatedAt)).orElse(null);
        Long sessionId=null;long openCount=0;long readyCount=0;
        if(reservation!=null&&reservation.getStatus()==ReservationStatus.CHECKED_IN){
            sessionId=sessions.findByReservationId(reservation.getId()).map(ServiceSession::getId).orElse(null);
            if(sessionId!=null){openCount=orders.countByServiceSessionIdAndStatusIn(sessionId,OPEN);readyCount=orders.countByServiceSessionIdAndStatus(sessionId,DiningOrderStatus.READY);}
        }
        TableServiceState state=state(table,openCount,readyCount);
        return new TableOverview(table.getId(),table.getCode(),table.getName(),table.getFloor(),table.getArea(),table.getSeats(),table.getStatus(),table.isActive(),state,
                reservation==null?null:reservation.getId(),reservation==null?null:reservation.getCode(),reservation==null?null:reservation.getCustomerName(),
                reservation==null?null:reservation.getPhone(),reservation==null?null:reservation.getPartySize(),sessionId,openCount,readyCount);
    }

    private TableServiceState state(RestaurantTable table,long openCount,long readyCount){
        if(table.getStatus()==TableStatus.INACTIVE)return TableServiceState.INACTIVE;
        if(table.getStatus()==TableStatus.NEEDS_CLEANING)return TableServiceState.NEEDS_CLEANING;
        if(table.getStatus()==TableStatus.RESERVED)return TableServiceState.RESERVED;
        if(table.getStatus()==TableStatus.AVAILABLE)return TableServiceState.EMPTY;
        if(readyCount>0)return TableServiceState.NEEDS_SERVING;
        if(openCount>0)return TableServiceState.WAITING_KITCHEN;
        return TableServiceState.DINING;
    }
}
