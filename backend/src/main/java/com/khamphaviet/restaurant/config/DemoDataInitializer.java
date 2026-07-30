package com.khamphaviet.restaurant.config;

import com.khamphaviet.restaurant.auth.*;
import com.khamphaviet.restaurant.service.*;
import com.khamphaviet.restaurant.table.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
@Profile("demo")
public class DemoDataInitializer implements ApplicationRunner {
    private final AppUserRepository users;
    private final RestaurantTableRepository tables;
    private final StaffShiftRepository shifts;
    private final PasswordEncoder encoder;

    public DemoDataInitializer(AppUserRepository users, RestaurantTableRepository tables,
            StaffShiftRepository shifts, PasswordEncoder encoder) {
        this.users = users; this.tables = tables; this.shifts = shifts; this.encoder = encoder;
    }

    @Override public void run(ApplicationArguments args) {
        createUserIfMissing("Quản trị viên", "admin@khamphaviet.vn", "Admin@123", UserRole.ADMIN);
        createUserIfMissing("Nguyễn Minh Anh", "staff@khamphaviet.vn", "Staff@123", UserRole.STAFF);
        createUserIfMissing("Hoàng Văn Nam", "staff2@khamphaviet.vn", "Staff2@123", UserRole.STAFF);
        createUserIfMissing("Giàng A Páo", "staff3@khamphaviet.vn", "Staff3@123", UserRole.STAFF);
        createUserIfMissing("Lê Thị Hương", "staff4@khamphaviet.vn", "Staff4@123", UserRole.STAFF);
        createUserIfMissing("Lò Thị Mai", "kitchen@khamphaviet.vn", "Kitchen@123", UserRole.KITCHEN);
        createUserIfMissing("Trần Thu Hà", "customer@khamphaviet.vn", "Customer@123", UserRole.CUSTOMER);
        if (shifts.count() == 0) {
            Instant now = Instant.now();
            users.findByRoleAndActiveTrueOrderByFullNameAsc(UserRole.STAFF).forEach(staff -> {
                StaffShift shift = new StaffShift(staff, now.minusSeconds(3600), now.plusSeconds(8 * 3600), "demo");
                shift.start();
                shifts.save(shift);
            });
        }
        if (tables.count() == 0) {
            for (int i = 1; i <= 22; i++) {
                int column = (i - 1) % 6, row = (i - 1) / 6;
                String area = i <= 6 ? "Cửa sổ" : i <= 14 ? "Sảnh chính" : i <= 18 ? "Gia đình" : "Riêng tư";
                int seats = i % 5 == 0 ? 8 : i % 3 == 0 ? 6 : 4;
                tables.save(new RestaurantTable("B" + String.format("%02d", i), "Bàn " + i, "Tầng trệt",
                        area, seats, 8 + column * 15, 12 + row * 23, i % 4 == 0 ? "RECTANGLE" : "ROUND"));
            }
        }
    }

    private void createUserIfMissing(String name, String email, String password, UserRole role) {
        if (users.findByEmailIgnoreCase(email).isEmpty()) users.save(new AppUser(name, email, encoder.encode(password), role));
    }
}
