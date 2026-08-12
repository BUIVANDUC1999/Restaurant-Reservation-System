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
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class OperationalTimeoutService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final OperationalTimeoutRepository timeouts;
    private final OperationalTimeoutEventRepository timeoutEvents;
    private final OperationalTimePolicy policy;
    private final ReservationRepository reservations;
    private final ReservationDepositRepository deposits;
    private final ReservationTableAssignmentRepository assignments;
    private final DiningOrderRepository orders;
    private final DiningOrderItemRepository orderItems;
    private final ServiceSessionRepository sessions;
    private final TableServiceRequestRepository serviceRequests;
    private final RestaurantTableRepository tables;
    private final NotificationService notifications;

    public OperationalTimeoutService(OperationalTimeoutRepository timeouts,
                                     OperationalTimeoutEventRepository timeoutEvents, OperationalTimePolicy policy,
                                     ReservationRepository reservations, ReservationDepositRepository deposits,
                                     ReservationTableAssignmentRepository assignments,
                                     DiningOrderRepository orders, DiningOrderItemRepository orderItems,
                                     ServiceSessionRepository sessions, TableServiceRequestRepository serviceRequests,
                                     RestaurantTableRepository tables, NotificationService notifications) {
        this.timeouts = timeouts; this.timeoutEvents = timeoutEvents; this.policy = policy;
        this.reservations = reservations; this.deposits = deposits;
        this.assignments = assignments; this.orders = orders; this.orderItems = orderItems;
        this.sessions = sessions; this.serviceRequests = serviceRequests;
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
    public OperationalTimeout resolve(Long id, String note, String actor) {
        OperationalTimeout timeout = timeouts.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy cảnh báo quá hạn"));
        timeout.resolve(note, actor);
        event(timeout, "RESOLVED", actor, timeout.getAssignedTo(), timeout.getAssignedTo(), note);
        return timeout;
    }

    @Transactional
    public OperationalTimeout assign(Long id, String assignee, String note, String actor) {
        if (assignee == null || assignee.isBlank()) throw new BusinessException("Cần chọn người phụ trách");
        OperationalTimeout timeout = find(id);
        String previous = timeout.getAssignedTo();
        timeout.assign(assignee);
        event(timeout, previous == null ? "ASSIGNED" : "TRANSFERRED", actor, previous, assignee, note);
        return timeout;
    }

    @Transactional
    public OperationalTimeout acknowledge(Long id, String actor) {
        OperationalTimeout timeout = find(id);
        String previous = timeout.getAssignedTo();
        timeout.acknowledge(actor);
        event(timeout, "ACKNOWLEDGED", actor, previous, timeout.getAssignedTo(), null);
        return timeout;
    }

    public List<OperationalTimeoutEvent> events(Long id) {
        find(id);
        return timeoutEvents.findByTimeoutIdOrderByCreatedAtDesc(id);
    }

    private OperationalTimeout find(Long id) {
        return timeouts.findById(id).orElseThrow(() -> new BusinessException("Không tìm thấy cảnh báo quá hạn"));
    }

    private void event(OperationalTimeout timeout, String action, String actor,
                       String from, String to, String note) {
        timeoutEvents.save(new OperationalTimeoutEvent(timeout.getId(), action, actor, from, to, note));
    }

    @Scheduled(fixedDelayString = "${app.timeouts.monitor-delay-ms:60000}", initialDelayString = "15000")
    @Transactional
    public void monitor() {
        Instant now = Instant.now();
        monitorReservationHolds(now);
        monitorReservationConfirmations(now);
        monitorLateCustomers(now);
        monitorKitchen(now);
        monitorServiceRequests(now);
        monitorCleaning(now);
    }

    private void monitorReservationConfirmations(Instant now) {
        for (Reservation reservation : reservations.findByStatusIn(List.of(ReservationStatus.PENDING))) {
            ReservationDeposit deposit = deposits.findByReservationId(reservation.getId()).orElse(null);
            if (deposit == null || deposit.getStatus() != DepositStatus.PAID || deposit.getPaidAt() == null) continue;
            Instant deadline = deposit.getPaidAt().plusSeconds(policy.getReservationConfirmationMinutes() * 60L);
            if (deadline.isAfter(now)) continue;
            long overdue = Duration.between(deadline, now).toMinutes();
            TimeoutSeverity severity = overdue >= policy.getReservationConfirmationMinutes()
                    ? TimeoutSeverity.CRITICAL : TimeoutSeverity.WARNING;
            String key = "confirmation-" + reservation.getId() + "-" + deposit.getPaidAt().toEpochMilli();
            open(key, TimeoutType.RESERVATION_CONFIRMATION, severity, "RESERVATION", reservation.getId(),
                    reservation.getId(), null, "Chờ xác nhận cọc",
                    reservation.getCode() + " · chậm " + Math.max(1, overdue) + "p",
                    deadline);
            notifications.createStaffAlert(reservation.getId(), NotificationType.TIMEOUT,
                    "Cọc chờ xác nhận", reservation.getCode() + " · xác nhận đơn",
                    "timeout-" + key);
        }
        for (Reservation reservation : reservations.findByStatusIn(List.of(
                ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN, ReservationStatus.COMPLETED,
                ReservationStatus.CANCELLED, ReservationStatus.REJECTED, ReservationStatus.NO_SHOW,
                ReservationStatus.EXPIRED))) {
            resolveOpen(TimeoutType.RESERVATION_CONFIRMATION, "RESERVATION", reservation.getId(),
                    "Đơn đặt bàn đã được nhân viên xử lý");
        }
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
                    "Hết hạn cọc", reservation.getCode() + " · chưa thanh toán", reservation.getHoldExpiresAt());
            reservation.changeStatus(ReservationStatus.EXPIRED);
            assignments.deleteByReservationId(reservation.getId());
            timeout.resolve("Hệ thống tự giải phóng lượt giữ bàn chưa đặt cọc");
            notifications.createStaffAlert(reservation.getId(), NotificationType.TIMEOUT, "Đã nhả bàn",
                    reservation.getCode() + " · hết hạn cọc", "timeout-" + key);
        }
    }

    private void monitorLateCustomers(Instant now) {
        for (Reservation reservation : reservations.findByStatusIn(List.of(ReservationStatus.CONFIRMED))) {
            if (reservation.getSource() == ReservationSource.WALK_IN) continue;
            Instant arrival = LocalDateTime.of(reservation.getReservationDate(), reservation.effectiveTime())
                    .atZone(ZONE).toInstant();
            long lateMinutes = Duration.between(arrival, now).toMinutes();
            if (lateMinutes < policy.getLateWarningMinutes()) continue;
            TimeoutSeverity severity = lateMinutes >= policy.getLateCriticalMinutes()
                    ? TimeoutSeverity.CRITICAL : TimeoutSeverity.WARNING;
            String key = "late-" + reservation.getId() + "-" + arrival.toEpochMilli();
            open(key, TimeoutType.CUSTOMER_LATE, severity, "RESERVATION", reservation.getId(),
                    reservation.getId(), null, "Khách trễ",
                    reservation.getCode() + " · trễ " + lateMinutes + "p · giữ bàn/no-show",
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
            KitchenContext context = kitchenContext(item);
            String etaKey = item.getId() + "-" + item.getEstimatedReadyAt().toEpochMilli();
            if (item.getEstimatedReadyAt().isAfter(now)) {
                resolveOpen(TimeoutType.KITCHEN_SLA, "DINING_ORDER_ITEM", item.getId(), "Bếp đã cập nhật ETA mới");
                long secondsUntil = Duration.between(now, item.getEstimatedReadyAt()).getSeconds();
                if (secondsUntil <= policy.getKitchenPrewarningMinutes() * 60L) {
                    long minutesUntil = Math.max(1, (secondsUntil + 59) / 60);
                    notifications.createStaffAlert(context.reservationId(), NotificationType.KITCHEN_DELAY,
                            "Sắp trễ · " + context.tableLabel(),
                            itemLabel(item) + " · còn " + minutesUntil + "p",
                            "kitchen-prewarning-" + etaKey);
                }
                continue;
            }
            long overdue = Math.max(1, Duration.between(item.getEstimatedReadyAt(), now).toMinutes());
            TimeoutSeverity severity = overdue >= policy.getKitchenCriticalOverdueMinutes()
                    ? TimeoutSeverity.CRITICAL : TimeoutSeverity.WARNING;
            String key = "kitchen-" + etaKey;
            OperationalTimeout timeout = open(key, TimeoutType.KITCHEN_SLA, severity,
                    "DINING_ORDER_ITEM", item.getId(), context.reservationId(), context.tableId(),
                    "Món chậm · " + context.tableLabel(),
                    itemLabel(item) + " · chậm " + overdue + "p · " + context.staffLabel(),
                    item.getEstimatedReadyAt());
            assignKitchenTimeout(timeout, context.staffName());

            String stage;
            String title;
            String action;
            if (overdue >= policy.getKitchenCriticalOverdueMinutes()) {
                stage = "critical";
                title = "Món chậm · " + context.tableLabel();
                action = "Điều phối ngay";
            } else if (overdue >= policy.getKitchenWaiterEscalationMinutes()) {
                stage = "waiter";
                title = "Món chậm · " + context.tableLabel();
                action = context.staffName() == null || context.staffName().isBlank()
                        ? "Phân công báo khách"
                        : context.staffName() + " báo khách";
            } else {
                stage = "warning";
                title = "Món chậm · " + context.tableLabel();
                action = "Cập nhật ETA";
            }
            notifications.createStaffAlert(context.reservationId(), NotificationType.KITCHEN_DELAY,
                    title, itemLabel(item) + " · " + overdue + "p · " + action,
                    "kitchen-overdue-" + etaKey + "-" + stage);
        }
        for (DiningOrderItem item : orderItems.findByStatusIn(List.of(DiningOrderItemStatus.READY, DiningOrderItemStatus.SERVED))) {
            resolveOpen(TimeoutType.KITCHEN_SLA, "DINING_ORDER_ITEM", item.getId(), "Bếp đã cập nhật trạng thái món");
        }
    }

    private KitchenContext kitchenContext(DiningOrderItem item) {
        DiningOrder order = orders.findById(item.getOrderId()).orElse(null);
        ServiceSession session = order == null ? null : sessions.findById(order.getServiceSessionId()).orElse(null);
        Long reservationId = session == null ? null : session.getReservationId();
        List<Long> tableIds = reservationId == null ? List.of() : assignments.findByReservationId(reservationId).stream()
                .map(ReservationTableAssignment::getTableId).toList();
        String tableLabel = tableIds.isEmpty() ? "chưa gán bàn" : tables.findAllById(tableIds).stream()
                .map(RestaurantTable::getCode).sorted().collect(Collectors.joining(", "));
        if (tableLabel.isBlank()) tableLabel = "chưa gán bàn";
        String staffName = session == null ? null : session.getAssignedStaffName();
        String staffLabel = staffName == null || staffName.isBlank() ? "chưa phân công" : staffName;
        return new KitchenContext(reservationId, tableIds.isEmpty() ? null : tableIds.get(0),
                tableLabel, staffName, staffLabel);
    }

    private String itemLabel(DiningOrderItem item) {
        return item.getQuantity() + "× " + item.getItemNameSnapshot();
    }

    private void assignKitchenTimeout(OperationalTimeout timeout, String staffName) {
        if (staffName == null || staffName.isBlank() || Objects.equals(timeout.getAssignedTo(), staffName)) return;
        String previous = timeout.getAssignedTo();
        timeout.assign(staffName);
        event(timeout, previous == null ? "ASSIGNED" : "TRANSFERRED", "SYSTEM", previous, staffName,
                "Tự gán theo nhân viên đang phụ trách bàn");
    }

    private record KitchenContext(Long reservationId, Long tableId, String tableLabel,
                                  String staffName, String staffLabel) {}

    private void monitorServiceRequests(Instant now) {
        for (TableServiceRequest request : serviceRequests.findByStatusIn(List.of(TableRequestStatus.NEW))) {
            Instant deadline = request.getCreatedAt().plusSeconds(policy.getTableRequestAckMinutes() * 60L);
            if (deadline.isAfter(now)) continue;
            long overdue = Duration.between(deadline, now).toMinutes();
            TimeoutSeverity severity = overdue >= policy.getTableRequestAckMinutes()
                    ? TimeoutSeverity.CRITICAL : TimeoutSeverity.WARNING;
            open("request-" + request.getId(), TimeoutType.SERVICE_REQUEST_ACK, severity,
                    "TABLE_SERVICE_REQUEST", request.getId(), null, request.getTableId(),
                    "QR chưa nhận",
                    request.getType() + " · chậm " + Math.max(1, overdue) + "p", deadline);
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
                    "Dọn bàn chậm", table.getCode() + " · chậm " + Math.max(1, overdue) + "p", deadline);
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
