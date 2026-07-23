package com.khamphaviet.restaurant.reservation;

import com.khamphaviet.restaurant.common.BusinessException;
import com.khamphaviet.restaurant.deposit.*;
import com.khamphaviet.restaurant.table.*;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class TableSchedulingService {
    public static final int CLEANING_BUFFER_MINUTES = 15;
    private static final List<ReservationStatus> BLOCKING = List.of(
            ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN);
    private final RestaurantTableRepository tables;
    private final ReservationRepository reservations;
    private final ReservationTableAssignmentRepository assignments;
    private final ReservationDepositRepository deposits;

    public TableSchedulingService(RestaurantTableRepository tables, ReservationRepository reservations,
                                  ReservationTableAssignmentRepository assignments, ReservationDepositRepository deposits) {
        this.tables=tables; this.reservations=reservations; this.assignments=assignments; this.deposits=deposits;
    }

    public List<ReservationDtos.AvailableTableResponse> available(LocalDate date, LocalTime time, int durationMinutes, int partySize) {
        validateSchedule(date, time, durationMinutes);
        Set<Long> blocked = blockedTableIds(date, time, durationMinutes, null);
        return tables.findAllByOrderByFloorAscCodeAsc().stream()
                .filter(t -> t.isActive() && t.getStatus() != TableStatus.INACTIVE && !blocked.contains(t.getId()))
                .map(t -> new ReservationDtos.AvailableTableResponse(t.getId(), t.getCode(), t.getName(), t.getArea(),
                        t.getSeats(), t.getLayoutX(), t.getLayoutY(), t.getShape()))
                .toList();
    }

    public List<RestaurantTable> validateSelection(LocalDate date, LocalTime time, int durationMinutes, int partySize,
                                                   List<Long> requestedIds, Long ignoredReservationId) {
        if (requestedIds == null || requestedIds.isEmpty()) throw new BusinessException("Hãy chọn ít nhất một bàn");
        List<Long> ids=requestedIds.stream().distinct().toList();
        List<RestaurantTable> selected=tables.findAllById(ids);
        if(selected.size()!=ids.size()) throw new BusinessException("Có bàn không tồn tại");
        if(selected.stream().anyMatch(t -> !t.isActive() || t.getStatus()==TableStatus.INACTIVE))
            throw new BusinessException("Có bàn đang tạm ngưng sử dụng");
        Set<Long> blocked=blockedTableIds(date,time,durationMinutes,ignoredReservationId);
        if(ids.stream().anyMatch(blocked::contains)) throw new BusinessException("Bàn vừa được khách khác chọn trong khung giờ này");
        if(selected.stream().mapToInt(RestaurantTable::getSeats).sum()<partySize)
            throw new BusinessException("Tổng số ghế của bàn đã chọn chưa đủ");
        return selected;
    }

    public void validateSchedule(LocalDate date, LocalTime time, int durationMinutes) {
        LocalDateTime start=LocalDateTime.of(date,time);
        if(start.isBefore(LocalDateTime.now().minusMinutes(1))) throw new BusinessException("Giờ đặt bàn không được ở trong quá khứ");
        if(time.isBefore(LocalTime.of(10,0)) || time.isAfter(LocalTime.of(21,30)))
            throw new BusinessException("Nhà hàng nhận khách từ 10:00 đến 21:30");
        if(durationMinutes<60 || durationMinutes>300) throw new BusinessException("Thời lượng dùng bàn phải từ 60 đến 300 phút");
        if(start.plusMinutes(durationMinutes).isAfter(date.atTime(23,30)))
            throw new BusinessException("Thời gian kết thúc không được sau 23:30");
    }

    private Set<Long> blockedTableIds(LocalDate date, LocalTime start, int durationMinutes, Long ignoredReservationId) {
        LocalTime end=start.plusMinutes(durationMinutes+CLEANING_BUFFER_MINUTES);
        List<Reservation> sameDay=reservations.findByReservationDateAndStatusIn(date,BLOCKING).stream()
                .filter(r -> !Objects.equals(r.getId(),ignoredReservationId))
                .filter(this::stillBlocks)
                .filter(r -> overlaps(start,end,r.effectiveTime(),r.effectiveTime().plusMinutes(r.effectiveDurationMinutes()+CLEANING_BUFFER_MINUTES)))
                .toList();
        if(sameDay.isEmpty()) return Set.of();
        Set<Long> reservationIds=sameDay.stream().map(Reservation::getId).collect(java.util.stream.Collectors.toSet());
        return assignments.findByReservationIdIn(reservationIds.stream().toList()).stream()
                .map(ReservationTableAssignment::getTableId).collect(java.util.stream.Collectors.toSet());
    }

    private boolean stillBlocks(Reservation reservation) {
        if(reservation.getStatus()!=ReservationStatus.PENDING) return true;
        boolean paid=deposits.findByReservationId(reservation.getId()).map(d -> d.getStatus()==DepositStatus.PAID).orElse(false);
        return paid || reservation.getHoldExpiresAt()==null || reservation.getHoldExpiresAt().isAfter(Instant.now());
    }

    private boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }
}
