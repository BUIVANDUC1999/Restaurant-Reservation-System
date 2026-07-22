package com.khamphaviet.restaurant.report;

import com.khamphaviet.restaurant.billing.Payment;
import com.khamphaviet.restaurant.billing.PaymentRepository;
import com.khamphaviet.restaurant.reservation.ReservationRepository;
import com.khamphaviet.restaurant.reservation.ReservationStatus;
import com.khamphaviet.restaurant.service.ServiceSessionRepository;
import com.khamphaviet.restaurant.service.ServiceSessionStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.*;

@RestController
@RequestMapping("/api/v1/admin/reports")
public class AdminReportController {
    private static final ZoneId RESTAURANT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ReservationRepository reservations;
    private final ServiceSessionRepository sessions;
    private final PaymentRepository payments;

    public AdminReportController(ReservationRepository reservations, ServiceSessionRepository sessions, PaymentRepository payments) {
        this.reservations = reservations;
        this.sessions = sessions;
        this.payments = payments;
    }

    public record OperationsSummary(long reservationsToday, long pendingReservations, long activeSessions,
                                    long invoicesThisMonth, BigDecimal revenueToday, BigDecimal revenueThisMonth) {}

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
}
