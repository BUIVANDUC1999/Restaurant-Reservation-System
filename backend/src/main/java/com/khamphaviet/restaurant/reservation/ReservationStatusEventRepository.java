package com.khamphaviet.restaurant.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationStatusEventRepository extends JpaRepository<ReservationStatusEvent, Long> {
    List<ReservationStatusEvent> findByReservationIdOrderByCreatedAtDesc(Long reservationId);
}
