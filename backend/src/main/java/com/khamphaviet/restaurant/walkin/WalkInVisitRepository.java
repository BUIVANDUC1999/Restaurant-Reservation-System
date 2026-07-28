package com.khamphaviet.restaurant.walkin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface WalkInVisitRepository extends JpaRepository<WalkInVisit,Long> {
    List<WalkInVisit> findAllByOrderByArrivedAtDesc();
    List<WalkInVisit> findByStatusIn(List<WalkInStatus> statuses);
}
