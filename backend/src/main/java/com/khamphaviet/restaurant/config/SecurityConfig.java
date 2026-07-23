package com.khamphaviet.restaurant.config;

import com.khamphaviet.restaurant.auth.JwtAuthenticationFilter;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/actuator/health", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/menu/**").permitAll()
                        .requestMatchers("/api/v1/reservations/**").permitAll()
                        .requestMatchers("/api/v1/table-guest/**").permitAll()
                        .requestMatchers("/api/v1/customer/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/kitchen/**").hasAnyRole("ADMIN", "KITCHEN")
                        .requestMatchers("/api/v1/staff/checkouts/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/v1/staff/**").hasAnyRole("ADMIN", "STAFF")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class).build();
    }
}
