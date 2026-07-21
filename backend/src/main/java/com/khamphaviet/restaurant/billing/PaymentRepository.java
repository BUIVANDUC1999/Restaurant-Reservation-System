package com.khamphaviet.restaurant.billing;
import org.springframework.data.jpa.repository.JpaRepository;import java.util.Optional;
public interface PaymentRepository extends JpaRepository<Payment,Long>{Optional<Payment> findByServiceSessionId(Long serviceSessionId);boolean existsByServiceSessionIdAndStatus(Long serviceSessionId,PaymentStatus status);}
