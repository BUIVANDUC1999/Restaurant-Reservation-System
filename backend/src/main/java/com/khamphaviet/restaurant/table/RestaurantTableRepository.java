package com.khamphaviet.restaurant.table;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {
    List<RestaurantTable> findAllByOrderByFloorAscCodeAsc();
    boolean existsByCodeIgnoreCase(String code);
    Optional<RestaurantTable> findByPublicToken(String publicToken);
    List<RestaurantTable> findByStatus(TableStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RestaurantTable t where t.id=:id")
    Optional<RestaurantTable> findByIdForUpdate(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RestaurantTable t where t.id in :ids order by t.id")
    List<RestaurantTable> findAllByIdForUpdate(@Param("ids") List<Long> ids);
}

