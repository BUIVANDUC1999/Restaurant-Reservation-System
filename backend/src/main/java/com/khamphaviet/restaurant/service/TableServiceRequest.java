package com.khamphaviet.restaurant.service;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity @Table(name="table_service_requests") @Getter @NoArgsConstructor
public class TableServiceRequest {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long tableId;
    @Column(nullable=false) private Long serviceSessionId;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private TableRequestType type;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private TableRequestStatus status;
    @Column(length=300) private String note;
    @Column(nullable=false) private Instant createdAt;
    private Instant acknowledgedAt;
    private Instant completedAt;

    public TableServiceRequest(Long tableId,Long serviceSessionId,TableRequestType type,String note){
        this.tableId=tableId;this.serviceSessionId=serviceSessionId;this.type=type;
        this.note=note==null||note.isBlank()?null:note.trim();this.status=TableRequestStatus.NEW;this.createdAt=Instant.now();
    }
    public void change(TableRequestStatus next){
        this.status=next;
        if(next==TableRequestStatus.ACKNOWLEDGED)this.acknowledgedAt=Instant.now();
        if(next==TableRequestStatus.DONE||next==TableRequestStatus.CANCELLED)this.completedAt=Instant.now();
    }
}
