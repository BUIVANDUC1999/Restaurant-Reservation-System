package com.khamphaviet.restaurant.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity @Table(name="operational_notifications") @Getter @NoArgsConstructor
public class OperationalNotification {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private Long reservationId;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private NotificationType type;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private NotificationChannel channel;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private NotificationStatus status;
    @Column(length=180) private String recipient;
    @Column(nullable=false,length=180) private String title;
    @Column(nullable=false,length=1200) private String message;
    @Column(nullable=false,unique=true,length=180) private String dedupeKey;
    @Column(nullable=false) private Instant createdAt;
    private Instant sentAt;
    private Instant readAt;
    @Column(length=600) private String errorMessage;

    public OperationalNotification(Long reservationId, NotificationType type, NotificationChannel channel,
                                   String recipient, String title, String message, String dedupeKey) {
        this.reservationId=reservationId;this.type=type;this.channel=channel;this.recipient=recipient;
        this.title=title;this.message=message;this.dedupeKey=dedupeKey;
        this.status=NotificationStatus.PENDING;this.createdAt=Instant.now();
    }
    public void sent(NotificationStatus status){this.status=status;this.sentAt=Instant.now();this.errorMessage=null;}
    public void failed(String error){this.status=NotificationStatus.FAILED;this.errorMessage=error==null?null:error.substring(0,Math.min(600,error.length()));}
    public void read(){this.readAt=Instant.now();}
}
