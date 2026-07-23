package com.khamphaviet.restaurant.timeout;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.timeouts")
@Getter
@Setter
public class OperationalTimePolicy {
    private int reservationHoldMinutes = 10;
    private int cleaningBufferMinutes = 15;
    private int upcomingAlertMinutes = 30;
    private int lateWarningMinutes = 15;
    private int lateCriticalMinutes = 20;
    private int tableRequestAckMinutes = 3;
    private int kitchenCriticalOverdueMinutes = 10;
    private int cleaningTargetMinutes = 15;

    public Snapshot snapshot() {
        return new Snapshot(reservationHoldMinutes, cleaningBufferMinutes, upcomingAlertMinutes,
                lateWarningMinutes, lateCriticalMinutes, tableRequestAckMinutes,
                kitchenCriticalOverdueMinutes, cleaningTargetMinutes);
    }

    public record Snapshot(int reservationHoldMinutes, int cleaningBufferMinutes, int upcomingAlertMinutes,
                           int lateWarningMinutes, int lateCriticalMinutes, int tableRequestAckMinutes,
                           int kitchenCriticalOverdueMinutes, int cleaningTargetMinutes) {}
}
