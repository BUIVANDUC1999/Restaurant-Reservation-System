package com.khamphaviet.restaurant.timeout;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OperationalTimeoutRepository extends JpaRepository<OperationalTimeout, Long> {
    Optional<OperationalTimeout> findByDedupeKey(String dedupeKey);
    List<OperationalTimeout> findTop100ByOrderByStatusAscSeverityDescOpenedAtDesc();
    List<OperationalTimeout> findByTypeAndEntityTypeAndEntityIdAndStatus(
            TimeoutType type, String entityType, Long entityId, TimeoutStatus status);
}
