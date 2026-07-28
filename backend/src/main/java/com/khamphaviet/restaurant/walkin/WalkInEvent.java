package com.khamphaviet.restaurant.walkin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity @Table(name="walk_in_events") @Getter @NoArgsConstructor
public class WalkInEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long walkInVisitId;
    @Enumerated(EnumType.STRING) private WalkInStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private WalkInStatus toStatus;
    @Column(nullable=false,length=60) private String action;
    @Column(length=500) private String note;
    @Column(nullable=false,length=180) private String actor;
    @Column(nullable=false) private Instant createdAt;
    public WalkInEvent(Long visitId, WalkInStatus from, WalkInStatus to, String action, String note, String actor){
        this.walkInVisitId=visitId;this.fromStatus=from;this.toStatus=to;this.action=action;
        this.note=note==null||note.isBlank()?null:note.trim();this.actor=actor;this.createdAt=Instant.now();
    }
}
