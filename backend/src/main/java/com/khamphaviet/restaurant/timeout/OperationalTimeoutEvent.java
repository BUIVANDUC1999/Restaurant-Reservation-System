package com.khamphaviet.restaurant.timeout;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "operational_timeout_events")
@Getter
@NoArgsConstructor
public class OperationalTimeoutEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long timeoutId;
    @Column(nullable = false, length = 40) private String action;
    @Column(nullable = false, length = 180) private String actor;
    @Column(length = 180) private String fromAssignee;
    @Column(length = 180) private String toAssignee;
    @Column(length = 400) private String note;
    @Column(nullable = false) private Instant createdAt;

    public OperationalTimeoutEvent(Long timeoutId, String action, String actor,
                                   String fromAssignee, String toAssignee, String note) {
        this.timeoutId = timeoutId;
        this.action = action;
        this.actor = actor;
        this.fromAssignee = fromAssignee;
        this.toAssignee = toAssignee;
        this.note = note;
        this.createdAt = Instant.now();
    }
}
