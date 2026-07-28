package com.khamphaviet.restaurant.walkin;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.walk-in")
@Getter @Setter
public class WalkInPolicy {
    private int defaultWaitMinutes = 30;
    private int diningMinutes = 120;
    private int waitCriticalGraceMinutes = 5;
    private int offerWarningMinutes = 3;
    private int offerExpiryMinutes = 5;
    private int seatedWarningMinutes = 7;
    private int seatedCriticalMinutes = 10;
    private int paymentWarningMinutes = 3;
    private int paymentCriticalMinutes = 5;
}
