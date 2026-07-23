package com.khamphaviet.restaurant.timeout;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "operational_timeouts")
@Getter
@NoArgsConstructor
public class OperationalTimeout {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TimeoutType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TimeoutSeverity severity;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TimeoutStatus status;
    @Column(nullable = false, length = 40) private String entityType;
    @Column(nullable = false) private Long entityId;
    private Long reservationId;
    private Long tableId;
    @Column(nullable = false, length = 180) private String title;
    @Column(nullable = false, length = 1200) private String details;
    @Column(nullable = false) private Instant deadlineAt;
    @Column(nullable = false) private Instant openedAt;
    private Instant resolvedAt;
    @Column(length = 400) private String resolutionNote;
    @Column(nullable = false, unique = true, length = 220) private String dedupeKey;

    public OperationalTimeout(TimeoutType type, TimeoutSeverity severity, String entityType, Long entityId,
                              Long reservationId, Long tableId, String title, String details,
                              Instant deadlineAt, String dedupeKey) {
        this.type = type; this.severity = severity; this.status = TimeoutStatus.OPEN;
        this.entityType = entityType; this.entityId = entityId; this.reservationId = reservationId; this.tableId = tableId;
        this.title = title; this.details = details; this.deadlineAt = deadlineAt;
        this.openedAt = Instant.now(); this.dedupeKey = dedupeKey;
    }

    public void escalate(TimeoutSeverity next, String updatedDetails) {
        if (next == TimeoutSeverity.CRITICAL && severity == TimeoutSeverity.WARNING) {
            severity = next;
            if (status == TimeoutStatus.RESOLVED) {
                status = TimeoutStatus.OPEN;
                openedAt = Instant.now();
                resolvedAt = null;
                resolutionNote = null;
            }
        }
        if (updatedDetails != null && !updatedDetails.isBlank()) details = updatedDetails;
    }

    public void resolve(String note) {
        if (status == TimeoutStatus.RESOLVED) return;
        status = TimeoutStatus.RESOLVED;
        resolvedAt = Instant.now();
        resolutionNote = note == null || note.isBlank() ? "Đã xử lý" : note.trim();
    }
}
