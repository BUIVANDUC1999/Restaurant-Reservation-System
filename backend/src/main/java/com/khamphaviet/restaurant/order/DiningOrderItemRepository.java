package com.khamphaviet.restaurant.order;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiningOrderItemRepository extends JpaRepository<DiningOrderItem, Long> {
    List<DiningOrderItem> findByOrderIdInOrderByIdAsc(List<Long> orderIds);
    List<DiningOrderItem> findByStatusIn(List<DiningOrderItemStatus> statuses);
}
