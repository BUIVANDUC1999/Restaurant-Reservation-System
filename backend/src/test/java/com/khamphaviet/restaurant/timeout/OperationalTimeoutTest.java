package com.khamphaviet.restaurant.timeout;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class OperationalTimeoutTest {
    private OperationalTimeout timeout() {
        return new OperationalTimeout(TimeoutType.CUSTOMER_LATE, TimeoutSeverity.WARNING,
                "RESERVATION", 7L, 7L, null, "Khách trễ", "Trễ 15 phút",
                Instant.now(), "late-" + System.nanoTime());
    }
    @Test
    void resolvedWarningReopensWhenItEscalatesToCritical() {
        OperationalTimeout timeout = new OperationalTimeout(TimeoutType.CUSTOMER_LATE, TimeoutSeverity.WARNING,
                "RESERVATION", 7L, 7L, null, "Khách trễ", "Trễ 15 phút",
                Instant.now(), "late-7");
        timeout.resolve("Đã gọi khách");

        timeout.escalate(TimeoutSeverity.CRITICAL, "Trễ trên 20 phút");

        assertEquals(TimeoutStatus.OPEN, timeout.getStatus());
        assertEquals(TimeoutSeverity.CRITICAL, timeout.getSeverity());
        assertNull(timeout.getResolvedAt());
        assertEquals("Trễ trên 20 phút", timeout.getDetails());
    }

    @Test
    void acknowledgeAutomaticallyAssignsTheIncident() {
        OperationalTimeout timeout = timeout();
        timeout.acknowledge("staff@khamphaviet.vn");
        assertEquals("staff@khamphaviet.vn", timeout.getAssignedTo());
        assertEquals("staff@khamphaviet.vn", timeout.getAcknowledgedBy());
        assertNotNull(timeout.getAssignedAt());
        assertNotNull(timeout.getAcknowledgedAt());
    }

    @Test
    void assignmentCanBeTransferred() {
        OperationalTimeout timeout = timeout();
        timeout.assign("staff-a@khamphaviet.vn");
        timeout.assign("staff-b@khamphaviet.vn");
        assertEquals("staff-b@khamphaviet.vn", timeout.getAssignedTo());
    }

    @Test
    void resolvingRecordsAccountability() {
        OperationalTimeout timeout = timeout();
        timeout.resolve("Đã gọi xác nhận", "manager@khamphaviet.vn");
        assertEquals(TimeoutStatus.RESOLVED, timeout.getStatus());
        assertEquals("manager@khamphaviet.vn", timeout.getResolvedBy());
        assertEquals("manager@khamphaviet.vn", timeout.getAcknowledgedBy());
    }
}
