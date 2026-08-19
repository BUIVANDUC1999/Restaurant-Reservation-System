package com.khamphaviet.restaurant.report;

import com.khamphaviet.restaurant.billing.Payment;
import com.khamphaviet.restaurant.billing.PaymentMethod;
import com.khamphaviet.restaurant.billing.PaymentRepository;
import com.khamphaviet.restaurant.reservation.Reservation;
import com.khamphaviet.restaurant.reservation.ReservationSource;
import com.khamphaviet.restaurant.reservation.ReservationRepository;
import com.khamphaviet.restaurant.reservation.ReservationStatus;
import com.khamphaviet.restaurant.reservation.ReservationTableAssignment;
import com.khamphaviet.restaurant.reservation.ReservationTableAssignmentRepository;
import com.khamphaviet.restaurant.service.ServiceSession;
import com.khamphaviet.restaurant.service.ServiceSessionRepository;
import com.khamphaviet.restaurant.service.ServiceSessionStatus;
import com.khamphaviet.restaurant.table.RestaurantTable;
import com.khamphaviet.restaurant.table.RestaurantTableRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.*;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/reports")
public class AdminReportController {
    private static final ZoneId RESTAURANT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ReservationRepository reservations;
    private final ServiceSessionRepository sessions;
    private final PaymentRepository payments;
    private final ReservationTableAssignmentRepository assignments;
    private final RestaurantTableRepository tables;

    public AdminReportController(ReservationRepository reservations, ServiceSessionRepository sessions,
                                 PaymentRepository payments, ReservationTableAssignmentRepository assignments,
                                 RestaurantTableRepository tables) {
        this.reservations = reservations;
        this.sessions = sessions;
        this.payments = payments;
        this.assignments = assignments;
        this.tables = tables;
    }

    public record OperationsSummary(long reservationsToday, long pendingReservations, long activeSessions,
                                    long invoicesThisMonth, BigDecimal revenueToday, BigDecimal revenueThisMonth) {}
    public record ReservationDetail(Long id, String code, String customerName, String phone, int partySize,
                                    LocalTime reservationTime, ReservationStatus status, ReservationSource source,
                                    List<String> tableCodes) {}
    public record ActiveSessionDetail(Long serviceSessionId, String reservationCode, String customerName,
                                      int partySize, List<String> tableCodes, String assignedStaffName,
                                      Instant openedAt) {}
    public record PaymentDetail(Long id, String invoiceCode, String reservationCode, String customerName,
                                BigDecimal totalAmount, PaymentMethod method, Instant paidAt) {}
    public record OperationsDetails(List<ReservationDetail> reservationsToday,
                                    List<ActiveSessionDetail> activeSessions,
                                    List<PaymentDetail> paymentsToday,
                                    List<PaymentDetail> paymentsThisMonth) {}

    @GetMapping("/operations")
    public OperationsSummary operations() {
        LocalDate today = LocalDate.now(RESTAURANT_ZONE);
        Instant startOfToday = today.atStartOfDay(RESTAURANT_ZONE).toInstant();
        Instant startOfTomorrow = today.plusDays(1).atStartOfDay(RESTAURANT_ZONE).toInstant();
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay(RESTAURANT_ZONE).toInstant();
        var todayPayments = payments.findByPaidAtGreaterThanEqualAndPaidAtLessThan(startOfToday, startOfTomorrow);
        var monthPayments = payments.findByPaidAtGreaterThanEqualAndPaidAtLessThan(startOfMonth, startOfTomorrow);
        return new OperationsSummary(
                reservations.countByReservationDate(today),
                reservations.countByStatus(ReservationStatus.PENDING),
                sessions.countByStatus(ServiceSessionStatus.ACTIVE),
                monthPayments.size(),
                todayPayments.stream().map(Payment::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                monthPayments.stream().map(Payment::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @GetMapping("/operations/details")
    public OperationsDetails operationDetails() {
        LocalDate today = LocalDate.now(RESTAURANT_ZONE);
        Instant startOfToday = today.atStartOfDay(RESTAURANT_ZONE).toInstant();
        Instant startOfTomorrow = today.plusDays(1).atStartOfDay(RESTAURANT_ZONE).toInstant();
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay(RESTAURANT_ZONE).toInstant();
        List<Reservation> todayReservations = reservations.findByReservationDateOrderByReservationTimeAsc(today);
        List<ServiceSession> activeSessions = sessions.findByStatus(ServiceSessionStatus.ACTIVE);
        Map<Long, List<String>> tableCodes = tableCodesFor(todayReservations.stream().map(Reservation::getId).toList());
        Map<Long, Reservation> activeReservations = reservations.findAllById(
                        activeSessions.stream().map(ServiceSession::getReservationId).toList()).stream()
                .collect(Collectors.toMap(Reservation::getId, Function.identity()));

        List<ReservationDetail> reservationDetails = todayReservations.stream()
                .map(reservation -> new ReservationDetail(reservation.getId(), reservation.getCode(),
                        reservation.getCustomerName(), reservation.getPhone(), reservation.getPartySize(),
                        reservation.effectiveTime(), reservation.getStatus(), reservation.getSource(),
                        tableCodes.getOrDefault(reservation.getId(), List.of())))
                .toList();
        Map<Long, List<String>> activeTableCodes = tableCodesFor(activeSessions.stream()
                .map(ServiceSession::getReservationId).toList());
        List<ActiveSessionDetail> sessionDetails = activeSessions.stream()
                .map(session -> {
                    Reservation reservation = activeReservations.get(session.getReservationId());
                    return new ActiveSessionDetail(session.getId(), reservation.getCode(), reservation.getCustomerName(),
                            reservation.getPartySize(), activeTableCodes.getOrDefault(reservation.getId(), List.of()),
                            session.getAssignedStaffName(), session.getOpenedAt());
                })
                .sorted(Comparator.comparing(ActiveSessionDetail::openedAt))
                .toList();

        return new OperationsDetails(reservationDetails, sessionDetails,
                paymentDetails(payments.findByPaidAtGreaterThanEqualAndPaidAtLessThan(startOfToday, startOfTomorrow)),
                paymentDetails(payments.findByPaidAtGreaterThanEqualAndPaidAtLessThan(startOfMonth, startOfTomorrow)));
    }

    private Map<Long, List<String>> tableCodesFor(Collection<Long> reservationIds) {
        if (reservationIds.isEmpty()) return Map.of();
        List<ReservationTableAssignment> rows = assignments.findByReservationIdIn(reservationIds.stream().distinct().toList());
        Map<Long, String> codeByTableId = tables.findAllById(rows.stream().map(ReservationTableAssignment::getTableId).distinct().toList())
                .stream().collect(Collectors.toMap(RestaurantTable::getId, RestaurantTable::getCode));
        return rows.stream().filter(row -> codeByTableId.containsKey(row.getTableId()))
                .collect(Collectors.groupingBy(ReservationTableAssignment::getReservationId,
                        Collectors.mapping(row -> codeByTableId.get(row.getTableId()), Collectors.toList())));
    }

    private List<PaymentDetail> paymentDetails(List<Payment> source) {
        if (source.isEmpty()) return List.of();
        Map<Long, ServiceSession> sessionById = sessions.findAllById(source.stream().map(Payment::getServiceSessionId).toList())
                .stream().collect(Collectors.toMap(ServiceSession::getId, Function.identity()));
        Map<Long, Reservation> reservationById = reservations.findAllById(sessionById.values().stream()
                        .map(ServiceSession::getReservationId).toList()).stream()
                .collect(Collectors.toMap(Reservation::getId, Function.identity()));
        return source.stream().sorted(Comparator.comparing(Payment::getPaidAt).reversed()).map(payment -> {
            ServiceSession session = sessionById.get(payment.getServiceSessionId());
            Reservation reservation = reservationById.get(session.getReservationId());
            return new PaymentDetail(payment.getId(), payment.getInvoiceCode(), reservation.getCode(),
                    reservation.getCustomerName(), payment.getTotalAmount(), payment.getMethod(), payment.getPaidAt());
        }).toList();
    }
}
