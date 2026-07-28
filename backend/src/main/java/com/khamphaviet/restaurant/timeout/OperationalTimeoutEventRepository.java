package com.khamphaviet.restaurant.timeout;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OperationalTimeoutEventRepository extends JpaRepository<OperationalTimeoutEvent, Long> {
    List<OperationalTimeoutEvent> findByTimeoutIdOrderByCreatedAtDesc(Long timeoutId);
}
