package com.khamphaviet.restaurant.auth;

import com.khamphaviet.restaurant.common.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer/profile")
public class CustomerProfileController {
    private final AppUserRepository users;

    public CustomerProfileController(AppUserRepository users) {
        this.users = users;
    }

    public record Profile(Long id, String fullName, String email, String phone) {}
    public record UpdateRequest(@NotBlank @Size(max = 120) String fullName,
                                @NotBlank @Pattern(regexp = "^[0-9+ ]{9,15}$") String phone) {}

    @GetMapping
    public Profile get(Authentication authentication) {
        return profile(find(authentication));
    }

    @PatchMapping
    @Transactional
    public Profile update(Authentication authentication, @Valid @RequestBody UpdateRequest request) {
        AppUser user = find(authentication);
        user.updateProfile(request.fullName().trim(), request.phone().trim());
        return profile(user);
    }

    private AppUser find(Authentication authentication) {
        return users.findByEmailIgnoreCase(authentication.getName())
                .filter(AppUser::isActive)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản khách hàng"));
    }

    private Profile profile(AppUser user) {
        return new Profile(user.getId(), user.getFullName(), user.getEmail(), user.getPhone());
    }
}
