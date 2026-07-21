package com.khamphaviet.restaurant.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    long countByRole(UserRole role);
    long countByRoleIn(List<UserRole> roles);
    long countByActiveTrue();
    List<AppUser> findAllByOrderByCreatedAtDesc();
}
