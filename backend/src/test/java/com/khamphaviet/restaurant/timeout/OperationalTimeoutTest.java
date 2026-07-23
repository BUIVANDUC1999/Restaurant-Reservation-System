package com.khamphaviet.restaurant.timeout;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class OperationalTimeoutTest {
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
}
