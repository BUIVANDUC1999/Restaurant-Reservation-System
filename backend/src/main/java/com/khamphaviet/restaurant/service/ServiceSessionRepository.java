package com.khamphaviet.restaurant.service;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ServiceSessionRepository extends JpaRepository<ServiceSession, Long> {
    Optional<ServiceSession> findByReservationId(Long reservationId);
    List<ServiceSession> findByReservationIdIn(List<Long> reservationIds);
    List<ServiceSession> findByStatus(ServiceSessionStatus status);
    long countByStatus(ServiceSessionStatus status);
}
