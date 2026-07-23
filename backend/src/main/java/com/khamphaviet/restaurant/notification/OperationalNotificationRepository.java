package com.khamphaviet.restaurant.notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OperationalNotificationRepository extends JpaRepository<OperationalNotification,Long> {
    boolean existsByDedupeKey(String dedupeKey);
    List<OperationalNotification> findTop100ByChannelOrderByCreatedAtDesc(NotificationChannel channel);
}
