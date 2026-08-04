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
            String[] areas = {"Cửa sổ", "Cửa sổ", "Sảnh ngoài", "Sảnh ngoài", "Sảnh ngoài", "Sảnh ngoài", "Trung tâm", "Trung tâm"};
            int[] seats = {4, 6, 4, 4, 6, 4, 6, 6};
            int[] x = {18, 50, 82, 18, 50, 82, 40, 60};
            int[] y = {15, 10, 15, 75, 82, 75, 46, 46};
            for (int i = 1; i <= 8; i++) {
                String name = i <= 6 ? "Bàn " + i : "Bàn trung tâm " + (i - 6);
                String shape = i == 2 || i == 5 || i >= 7 ? "RECTANGLE" : "ROUND";
                tables.save(new RestaurantTable("B" + String.format("%02d", i), name, "Tầng trệt",
                        areas[i - 1], seats[i - 1], x[i - 1], y[i - 1], shape));
            }
        }
    }

    private void createUserIfMissing(String name, String email, String password, UserRole role) {
        if (users.findByEmailIgnoreCase(email).isEmpty()) users.save(new AppUser(name, email, encoder.encode(password), role));
    }
}
