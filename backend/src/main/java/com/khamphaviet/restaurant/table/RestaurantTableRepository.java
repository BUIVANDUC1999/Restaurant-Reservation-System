package com.khamphaviet.restaurant.table;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {
    List<RestaurantTable> findAllByOrderByFloorAscCodeAsc();
    boolean existsByCodeIgnoreCase(String code);
    Optional<RestaurantTable> findByPublicToken(String publicToken);
}

