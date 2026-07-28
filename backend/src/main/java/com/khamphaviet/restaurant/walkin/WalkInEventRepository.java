package com.khamphaviet.restaurant.walkin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface WalkInEventRepository extends JpaRepository<WalkInEvent,Long> {
    List<WalkInEvent> findByWalkInVisitIdOrderByCreatedAtDesc(Long visitId);
}
