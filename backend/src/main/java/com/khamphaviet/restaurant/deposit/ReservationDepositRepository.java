package com.khamphaviet.restaurant.deposit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ReservationDepositRepository extends JpaRepository<ReservationDeposit,Long>{
    Optional<ReservationDeposit> findByReservationId(Long reservationId);
}
