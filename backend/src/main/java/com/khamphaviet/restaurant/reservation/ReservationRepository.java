package com.khamphaviet.restaurant.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findByCodeIgnoreCaseAndPhone(String code, String phone);
    List<Reservation> findByReservationDateAndTimeSlotAndStatusIn(LocalDate date, String timeSlot, List<ReservationStatus> statuses);
    List<Reservation> findAllByOrderByReservationDateDescCreatedAtDesc();
}

