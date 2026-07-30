package com.khamphaviet.restaurant.service;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WaiterAssignmentEventRepository extends JpaRepository<WaiterAssignmentEvent, Long> {
    List<WaiterAssignmentEvent> findTop50ByOrderByCreatedAtDesc();
    List<WaiterAssignmentEvent> findByServiceSessionIdOrderByCreatedAtDesc(Long serviceSessionId);
}
