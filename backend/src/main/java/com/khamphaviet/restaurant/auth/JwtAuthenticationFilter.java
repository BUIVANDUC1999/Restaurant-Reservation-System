package com.khamphaviet.restaurant.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService; private final AppUserRepository users;
    public JwtAuthenticationFilter(JwtService jwtService, AppUserRepository users) { this.jwtService = jwtService; this.users = users; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String email = jwtService.subject(header.substring(7));
                users.findByEmailIgnoreCase(email).filter(AppUser::isActive).ifPresent(user ->
                        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                                user.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))));
            } catch (RuntimeException ignored) { }
        }
        chain.doFilter(request, response);
    }
}

