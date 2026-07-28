package com.khamphaviet.restaurant.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByServiceSessionId(Long serviceSessionId);
    Optional<Payment> findByProviderOrderId(String providerOrderId);
    boolean existsByServiceSessionIdAndStatus(Long serviceSessionId, PaymentStatus status);
    List<Payment> findByPaidAtGreaterThanEqualAndPaidAtLessThan(Instant from, Instant to);
}
