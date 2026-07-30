package com.khamphaviet.restaurant.service;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface StaffShiftRepository extends JpaRepository<StaffShift, Long> {
    List<StaffShift> findByStartsAtLessThanAndEndsAtGreaterThanOrderByStartsAtAsc(Instant end, Instant start);
    List<StaffShift> findByStaffIdAndStatusIn(Long staffId, List<StaffShiftStatus> statuses);
    List<StaffShift> findByStatusAndStartsAtLessThanEqualAndEndsAtGreaterThanOrderByStartsAtAsc(
            StaffShiftStatus status, Instant startsAt, Instant endsAt);
}
