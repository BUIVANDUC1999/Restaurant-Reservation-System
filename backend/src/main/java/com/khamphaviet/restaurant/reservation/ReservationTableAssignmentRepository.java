package com.khamphaviet.restaurant.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReservationTableAssignmentRepository extends JpaRepository<ReservationTableAssignment, Long> {
    List<ReservationTableAssignment> findByReservationId(Long reservationId);
    List<ReservationTableAssignment> findByReservationIdIn(List<Long> reservationIds);
    List<ReservationTableAssignment> findByTableIdIn(List<Long> tableIds);
    void deleteByReservationId(Long reservationId);
}
