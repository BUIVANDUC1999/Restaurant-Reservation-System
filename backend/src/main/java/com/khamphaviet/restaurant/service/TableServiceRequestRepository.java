package com.khamphaviet.restaurant.service;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface TableServiceRequestRepository extends JpaRepository<TableServiceRequest,Long>{
    List<TableServiceRequest> findTop100ByOrderByCreatedAtDesc();
    List<TableServiceRequest> findByTableIdAndStatusInOrderByCreatedAtDesc(Long tableId,List<TableRequestStatus> statuses);
    long countByTableIdAndCreatedAtAfter(Long tableId, Instant after);
    List<TableServiceRequest> findByStatusIn(List<TableRequestStatus> statuses);
}
