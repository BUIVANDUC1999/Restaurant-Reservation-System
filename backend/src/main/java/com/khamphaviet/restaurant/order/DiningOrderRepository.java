package com.khamphaviet.restaurant.order;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiningOrderRepository extends JpaRepository<DiningOrder, Long> {
    List<DiningOrder> findByServiceSessionIdOrderByCreatedAtDesc(Long serviceSessionId);
    List<DiningOrder> findAllByOrderByCreatedAtDesc();
    boolean existsByServiceSessionIdAndStatusIn(Long serviceSessionId, List<DiningOrderStatus> statuses);
    long countByServiceSessionIdAndStatusIn(Long serviceSessionId, List<DiningOrderStatus> statuses);
    long countByServiceSessionIdAndStatus(Long serviceSessionId, DiningOrderStatus status);
    boolean existsByServiceSessionIdAndSource(Long serviceSessionId, OrderSource source);
}
