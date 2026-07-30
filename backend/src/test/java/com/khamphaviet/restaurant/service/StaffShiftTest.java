package com.khamphaviet.restaurant.service;

import com.khamphaviet.restaurant.auth.AppUser;
import com.khamphaviet.restaurant.auth.UserRole;
import com.khamphaviet.restaurant.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class StaffShiftTest {
    @Test
    void shiftMovesThroughControlledLifecycle() {
        StaffShift shift = shift();

        shift.start();
        assertEquals(StaffShiftStatus.ACTIVE, shift.getStatus());
        assertNotNull(shift.getActualStartedAt());

        shift.complete();
        assertEquals(StaffShiftStatus.COMPLETED, shift.getStatus());
        assertNotNull(shift.getActualEndedAt());
    }

    @Test
    void scheduledShiftCannotBeCompletedBeforeItStarts() {
        StaffShift shift = shift();

        assertThrows(BusinessException.class, shift::complete);
    }

    @Test
    void shiftEndMustBeAfterStart() {
        AppUser staff = staff();
        Instant now = Instant.now();

        assertThrows(BusinessException.class, () -> new StaffShift(staff, now, now, "admin@test.vn"));
    }

    private StaffShift shift() {
        Instant now = Instant.now();
        return new StaffShift(staff(), now.minusSeconds(60), now.plusSeconds(3600), "admin@test.vn");
    }

    private AppUser staff() {
        AppUser user = new AppUser("Nhân viên", "staff@test.vn", "hash", UserRole.STAFF);
        ReflectionTestUtils.setField(user, "id", 9L);
        return user;
    }
}
