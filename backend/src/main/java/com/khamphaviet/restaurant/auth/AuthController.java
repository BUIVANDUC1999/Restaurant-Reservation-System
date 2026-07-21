package com.khamphaviet.restaurant.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AppUserRepository users; private final PasswordEncoder encoder; private final JwtService jwt;
    public AuthController(AppUserRepository users, PasswordEncoder encoder, JwtService jwt) { this.users = users; this.encoder = encoder; this.jwt = jwt; }
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record RegisterRequest(@NotBlank @Size(max=120) String fullName,@NotBlank @Email String email,
                                  @NotBlank @Pattern(regexp="^[0-9+ ]{9,15}$") String phone,
                                  @NotBlank @Size(min=8,max=72) String password) {}
    public record LoginResponse(String accessToken, Long id, String fullName, String email, UserRole role) {}

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AppUser user = users.findByEmailIgnoreCase(request.email()).filter(AppUser::isActive)
                .filter(found -> encoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new com.khamphaviet.restaurant.common.BusinessException("Email hoặc mật khẩu không đúng"));
        return new LoginResponse(jwt.generate(user), user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }

    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        if(users.findByEmailIgnoreCase(request.email()).isPresent())
            throw new com.khamphaviet.restaurant.common.BusinessException("Email này đã được đăng ký");
        AppUser user=users.save(new AppUser(request.fullName().trim(),request.email().trim(),request.phone().trim(),encoder.encode(request.password()),UserRole.CUSTOMER));
        return new LoginResponse(jwt.generate(user),user.getId(),user.getFullName(),user.getEmail(),user.getRole());
    }
}
