package com.khamphaviet.restaurant.service;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ServiceSessionRepository extends JpaRepository<ServiceSession, Long> {
    Optional<ServiceSession> findByReservationId(Long reservationId);
    List<ServiceSession> findByReservationIdIn(List<Long> reservationIds);
    List<ServiceSession> findByStatus(ServiceSessionStatus status);
    long countByStatus(ServiceSessionStatus status);
    long countByAssignedStaffIdAndStatus(Long assignedStaffId, ServiceSessionStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ServiceSession s where s.id=:id")
    Optional<ServiceSession> findByIdForUpdate(@Param("id") Long id);
}
