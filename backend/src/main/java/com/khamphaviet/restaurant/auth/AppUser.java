package com.khamphaviet.restaurant.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "app_users")
@Getter
@NoArgsConstructor
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String fullName;
    @Column(nullable = false, unique = true) private String email;
    private String phone;
    @Column(nullable = false) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private UserRole role;
    @Column(nullable = false) private boolean active;
    @Column(nullable = false) private Instant createdAt;

    public AppUser(String fullName, String email, String passwordHash, UserRole role) {
        this(fullName, email, null, passwordHash, role);
    }
    public AppUser(String fullName, String email, String phone, String passwordHash, UserRole role) {
        this.fullName = fullName; this.email = email.toLowerCase(); this.passwordHash = passwordHash;
        this.phone = phone;
        this.role = role; this.active = true; this.createdAt = Instant.now();
    }
}
