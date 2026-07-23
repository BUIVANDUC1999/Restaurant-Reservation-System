package com.khamphaviet.restaurant.timeout;

import com.khamphaviet.restaurant.common.BusinessException;
import com.khamphaviet.restaurant.deposit.*;
import com.khamphaviet.restaurant.notification.*;
import com.khamphaviet.restaurant.order.*;
import com.khamphaviet.restaurant.reservation.*;
import com.khamphaviet.restaurant.service.*;
import com.khamphaviet.restaurant.table.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.Comparator;
import java.util.List;

@Service
public class OperationalTimeoutService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final OperationalTimeoutRepository timeouts;
    private final OperationalTimePolicy policy;
    private final ReservationRepository reservations;
    private final ReservationDepositRepository deposits;
    private final ReservationTableAssignmentRepository assignments;
    private final DiningOrderItemRepository orderItems;
    private final TableServiceRequestRepository serviceRequests;
    private final RestaurantTableRepository tables;
    private final NotificationService notifications;

    public OperationalTimeoutService(OperationalTimeoutRepository timeouts, OperationalTimePolicy policy,
                                     ReservationRepository reservations, ReservationDepositRepository deposits,
                                     ReservationTableAssignmentRepository assignments,
                                     DiningOrderItemRepository orderItems, TableServiceRequestRepository serviceRequests,
                                     RestaurantTableRepository tables, NotificationService notifications) {
        this.timeouts = timeouts; this.policy = policy; this.reservations = reservations; this.deposits = deposits;
        this.assignments = assignments; this.orderItems = orderItems; this.serviceRequests = serviceRequests;
        this.tables = tables; this.notifications = notifications;
    }

    public List<OperationalTimeout> list() {
        return timeouts.findTop100ByOrderByStatusAscSeverityDescOpenedAtDesc().stream()
                .sorted(Comparator.comparingInt((OperationalTimeout item) -> item.getStatus() == TimeoutStatus.OPEN ? 0 : 1)
                        .thenComparingInt(item -> item.getSeverity() == TimeoutSeverity.CRITICAL ? 0 : 1)
                        .thenComparing(OperationalTimeout::getOpenedAt, Comparator.reverseOrder()))
                .toList();
    }

    @Transactional
    public OperationalTimeout resolve(Long id, String note) {
        OperationalTimeout timeout = timeouts.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy cảnh báo quá hạn"));
        timeout.resolve(note);
        return timeout;
    }

    @Scheduled(fixedDelayString = "${app.timeouts.monitor-delay-ms:60000}", initialDelayString = "15000")
    @Transactional
    public void monitor() {
        Instant now = Instant.now();
        monitorReservationHolds(now);
        monitorLateCustomers(now);
        monitorKitchen(now);
        monitorServiceRequests(now);
        monitorCleaning(now);
    }

    private void monitorReservationHolds(Instant now) {
        for (Reservation reservation : reservations.findByStatusIn(List.of(ReservationStatus.PENDING))) {
            if (reservation.getHoldExpiresAt() == null || reservation.getHoldExpiresAt().isAfter(now)) continue;
            boolean paid = deposits.findByReservationId(reservation.getId())
                    .map(d -> d.getStatus() == DepositStatus.PAID).orElse(false);
            if (paid) continue;
            String key = "hold-" + reservation.getId() + "-" + reservation.getHoldExpiresAt().toEpochMilli();
            OperationalTimeout timeout = open(key, TimeoutType.RESERVATION_HOLD, TimeoutSeverity.CRITICAL,
                    "RESERVATION", reservation.getId(), reservation.getId(), null,
                    "Giữ bàn đã hết hạn", "Đơn " + reservation.getCode() + " chưa đặt cọc trong "
                            + policy.getReservationHoldMinutes() + " phút.", reservation.getHoldExpiresAt());
            reservation.changeStatus(ReservationStatus.EXPIRED);
            assignments.deleteByReservationId(reservation.getId());
            timeout.resolve("Hệ thống tự giải phóng lượt giữ bàn chưa đặt cọc");
            notifications.createStaffAlert(reservation.getId(), NotificationType.TIMEOUT, "Đã giải phóng bàn giữ quá hạn",
                    "Đơn " + reservation.getCode() + " chưa đặt cọc và đã tự động hết hạn.", "timeout-" + key);
        }
    }

    private void monitorLateCustomers(Instant now) {
        for (Reservation reservation : reservations.findByStatusIn(List.of(ReservationStatus.CONFIRMED))) {
            Instant arrival = LocalDateTime.of(reservation.getReservationDate(), reservation.effectiveTime())
                    .atZone(ZONE).toInstant();
            long lateMinutes = Duration.between(arrival, now).toMinutes();
            if (lateMinutes < policy.getLateWarningMinutes()) continue;
            TimeoutSeverity severity = lateMinutes >= policy.getLateCriticalMinutes()
                    ? TimeoutSeverity.CRITICAL : TimeoutSeverity.WARNING;
            String key = "late-" + reservation.getId() + "-" + arrival.toEpochMilli();
            open(key, TimeoutType.CUSTOMER_LATE, severity, "RESERVATION", reservation.getId(),
                    reservation.getId(), null, "Khách trễ hẹn",
                    "Đơn " + reservation.getCode() + " đã trễ " + lateMinutes
                            + " phút. Không tự hủy vì khách đã xác nhận; nhân viên quyết định giữ bàn hoặc no-show.",
                    arrival.plusSeconds(policy.getLateWarningMinutes() * 60L));
        }
        for (Reservation reservation : reservations.findByStatusIn(List.of(
                ReservationStatus.CHECKED_IN, ReservationStatus.COMPLETED, ReservationStatus.CANCELLED,
                ReservationStatus.REJECTED, ReservationStatus.NO_SHOW, ReservationStatus.EXPIRED))) {
            resolveOpen(TimeoutType.CUSTOMER_LATE, "RESERVATION", reservation.getId(), "Trạng thái đặt bàn đã được xử lý");
        }
    }

    private void monitorKitchen(Instant now) {
        for (DiningOrderItem item : orderItems.findByStatusIn(List.of(
                DiningOrderItemStatus.SUBMITTED, DiningOrderItemStatus.PREPARING, DiningOrderItemStatus.DELAYED))) {
            if (item.getEstimatedReadyAt().isAfter(now)) {
                resolveOpen(TimeoutType.KITCHEN_SLA, "DINING_ORDER_ITEM", item.getId(), "Bếp đã cập nhật ETA mới");
                continue;
            }
            long overdue = Duration.between(item.getEstimatedReadyAt(), now).toMinutes();
            TimeoutSeverity severity = overdue >= policy.getKitchenCriticalOverdueMinutes()
                    ? TimeoutSeverity.CRITICAL : TimeoutSeverity.WARNING;
            String key = "kitchen-" + item.getId() + "-" + item.getEstimatedReadyAt().toEpochMilli();
            open(key, TimeoutType.KITCHEN_SLA, severity, "DINING_ORDER_ITEM", item.getId(), null, null,
                    "Món quá thời gian dự kiến",
                    item.getItemNameSnapshot() + " đã quá SLA " + overdue + " phút. Bếp cần cập nhật trì hoãn hoặc hoàn tất món.",
                    item.getEstimatedReadyAt());
        }
        for (DiningOrderItem item : orderItems.findByStatusIn(List.of(DiningOrderItemStatus.READY, DiningOrderItemStatus.SERVED))) {
            resolveOpen(TimeoutType.KITCHEN_SLA, "DINING_ORDER_ITEM", item.getId(), "Bếp đã cập nhật trạng thái món");
        }
    }

    private void monitorServiceRequests(Instant now) {
        for (TableServiceRequest request : serviceRequests.findByStatusIn(List.of(TableRequestStatus.NEW))) {
            Instant deadline = request.getCreatedAt().plusSeconds(policy.getTableRequestAckMinutes() * 60L);
            if (deadline.isAfter(now)) continue;
            long overdue = Duration.between(deadline, now).toMinutes();
            TimeoutSeverity severity = overdue >= policy.getTableRequestAckMinutes()
                    ? TimeoutSeverity.CRITICAL : TimeoutSeverity.WARNING;
            open("request-" + request.getId(), TimeoutType.SERVICE_REQUEST_ACK, severity,
                    "TABLE_SERVICE_REQUEST", request.getId(), null, request.getTableId(),
                    "Yêu cầu tại bàn chưa được nhận",
                    "Yêu cầu " + request.getType() + " chưa có nhân viên nhận sau "
                            + (policy.getTableRequestAckMinutes() + overdue) + " phút.", deadline);
        }
        for (TableServiceRequest request : serviceRequests.findByStatusIn(List.of(
                TableRequestStatus.ACKNOWLEDGED, TableRequestStatus.DONE, TableRequestStatus.CANCELLED))) {
            resolveOpen(TimeoutType.SERVICE_REQUEST_ACK, "TABLE_SERVICE_REQUEST", request.getId(),
                    "Nhân viên đã tiếp nhận yêu cầu");
        }
    }

    private void monitorCleaning(Instant now) {
        for (RestaurantTable table : tables.findByStatus(TableStatus.NEEDS_CLEANING)) {
            Instant deadline = table.getStatusChangedAt().plusSeconds(policy.getCleaningTargetMinutes() * 60L);
            if (deadline.isAfter(now)) continue;
            long overdue = Duration.between(deadline, now).toMinutes();
            TimeoutSeverity severity = overdue >= policy.getCleaningTargetMinutes()
                    ? TimeoutSeverity.CRITICAL : TimeoutSeverity.WARNING;
            open("cleaning-" + table.getId() + "-" + table.getStatusChangedAt().toEpochMilli(),
                    TimeoutType.TABLE_CLEANING, severity, "RESTAURANT_TABLE", table.getId(), null, table.getId(),
                    "Bàn chờ dọn quá lâu", table.getCode() + " đã quá mục tiêu dọn bàn "
                            + overdue + " phút.", deadline);
        }
        for (RestaurantTable table : tables.findByStatus(TableStatus.AVAILABLE)) {
            resolveOpen(TimeoutType.TABLE_CLEANING, "RESTAURANT_TABLE", table.getId(), "Bàn đã sẵn sàng");
        }
    }

    private OperationalTimeout open(String key, TimeoutType type, TimeoutSeverity severity,
                                    String entityType, Long entityId, Long reservationId, Long tableId,
                                    String title, String details, Instant deadline) {
        return timeouts.findByDedupeKey(key).map(existing -> {
            existing.escalate(severity, details);
            return existing;
        }).orElseGet(() -> timeouts.save(new OperationalTimeout(type, severity, entityType, entityId,
                reservationId, tableId, title, details, deadline, key)));
    }

    private void resolveOpen(TimeoutType type, String entityType, Long entityId, String note) {
        timeouts.findByTypeAndEntityTypeAndEntityIdAndStatus(type, entityType, entityId, TimeoutStatus.OPEN)
                .forEach(timeout -> timeout.resolve(note));
    }
}
