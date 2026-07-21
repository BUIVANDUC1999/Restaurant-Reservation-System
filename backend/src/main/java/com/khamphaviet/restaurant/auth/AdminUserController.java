package com.khamphaviet.restaurant.auth;

import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private final AppUserRepository users;
    public AdminUserController(AppUserRepository users) { this.users = users; }

    public record UserStats(long totalCount, long adminCount, long employeeCount, long customerCount, long activeCount) {}
    public record UserSummary(Long id, String fullName, String email, UserRole role, boolean active, Instant createdAt) {}

    @GetMapping("/stats")
    public UserStats stats() {
        return new UserStats(users.count(), users.countByRole(UserRole.ADMIN),
                users.countByRoleIn(List.of(UserRole.STAFF, UserRole.KITCHEN)),
                users.countByRole(UserRole.CUSTOMER), users.countByActiveTrue());
    }

    @GetMapping
    public List<UserSummary> list() {
        return users.findAllByOrderByCreatedAtDesc().stream()
                .map(user -> new UserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole(), user.isActive(), user.getCreatedAt()))
                .toList();
    }
}
