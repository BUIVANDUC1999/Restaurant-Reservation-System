package com.khamphaviet.restaurant.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReservationItemRepository extends JpaRepository<ReservationItem, Long> {
    List<ReservationItem> findByReservationIdOrderByIdAsc(Long reservationId);
    List<ReservationItem> findByReservationIdInOrderByIdAsc(List<Long> reservationIds);
}

