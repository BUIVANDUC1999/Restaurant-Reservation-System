package com.khamphaviet.restaurant.config;

import com.khamphaviet.restaurant.auth.*;
import com.khamphaviet.restaurant.table.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoDataInitializer implements ApplicationRunner {
    private final AppUserRepository users; private final RestaurantTableRepository tables; private final PasswordEncoder encoder;
    public DemoDataInitializer(AppUserRepository users, RestaurantTableRepository tables, PasswordEncoder encoder) {
        this.users = users; this.tables = tables; this.encoder = encoder;
    }
    @Override public void run(ApplicationArguments args) {
        if (users.count() == 0) users.save(new AppUser("Quản trị viên", "admin@khamphaviet.vn", encoder.encode("Admin@123"), UserRole.ADMIN));
        if (tables.count() == 0) {
            for (int i = 1; i <= 10; i++) tables.save(new RestaurantTable("T1-" + String.format("%02d", i), "Bàn " + i, "Tầng 1", i <= 4 ? "Cửa sổ" : "Sảnh chính", i % 3 == 0 ? 8 : 6));
            for (int i = 1; i <= 12; i++) tables.save(new RestaurantTable("T2-" + String.format("%02d", i), "Bàn " + (10 + i), "Tầng 2", i <= 5 ? "Sân khấu" : "Sảnh tiệc", i % 4 == 0 ? 10 : 8));
        }
    }
}

