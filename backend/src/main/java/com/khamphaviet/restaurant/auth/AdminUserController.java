package com.khamphaviet.restaurant.auth;

import com.khamphaviet.restaurant.common.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
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
    public record ActiveRequest(@NotNull Boolean active) {}
    public record RoleRequest(@NotNull UserRole role) {}

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

    @PatchMapping("/{id}/active")
    @Transactional
    public UserSummary active(@PathVariable Long id, @Valid @RequestBody ActiveRequest request,
                              Authentication authentication) {
        AppUser user = find(id);
        if (user.getEmail().equalsIgnoreCase(authentication.getName()) && !request.active())
            throw new BusinessException("Bạn không thể tự khóa tài khoản đang đăng nhập");
        user.setActive(request.active());
        return summary(user);
    }

    @PatchMapping("/{id}/role")
    @Transactional
    public UserSummary role(@PathVariable Long id, @Valid @RequestBody RoleRequest request,
                            Authentication authentication) {
        AppUser user = find(id);
        if (user.getEmail().equalsIgnoreCase(authentication.getName()) && request.role() != UserRole.ADMIN)
            throw new BusinessException("Bạn không thể tự gỡ quyền quản trị viên");
        user.changeRole(request.role());
        return summary(user);
    }

    private AppUser find(Long id) {
        return users.findById(id).orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản"));
    }

    private UserSummary summary(AppUser user) {
        return new UserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.isActive(), user.getCreatedAt());
    }
}
