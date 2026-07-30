package com.khamphaviet.restaurant.service;

import com.khamphaviet.restaurant.auth.AppUser;
import com.khamphaviet.restaurant.auth.AppUserRepository;
import com.khamphaviet.restaurant.auth.UserRole;
import com.khamphaviet.restaurant.common.BusinessException;
import com.khamphaviet.restaurant.common.ConflictException;
import com.khamphaviet.restaurant.reservation.ReservationRepository;
import com.khamphaviet.restaurant.reservation.ReservationTableAssignmentRepository;
import com.khamphaviet.restaurant.table.RestaurantTableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WaiterAssignmentServiceTest {
    private ServiceSessionRepository sessions;
    private AppUserRepository users;
    private StaffShiftService shifts;
    private WaiterAssignmentEventRepository events;
    private WaiterAssignmentService service;

    @BeforeEach
    void setUp() {
        sessions = mock(ServiceSessionRepository.class);
        users = mock(AppUserRepository.class);
        shifts = mock(StaffShiftService.class);
        events = mock(WaiterAssignmentEventRepository.class);
        service = new WaiterAssignmentService(sessions, users, shifts, events,
                mock(ReservationRepository.class), mock(ReservationTableAssignmentRepository.class),
                mock(RestaurantTableRepository.class));
    }

    @Test
    void waiterCanClaimAnUnassignedSession() {
        AppUser waiter = user(7L, "Nguyễn Minh Anh", "staff@khamphaviet.vn", UserRole.STAFF);
        ServiceSession session = session(31L);
        when(users.findByEmailIgnoreCase(waiter.getEmail())).thenReturn(Optional.of(waiter));
        when(shifts.isOnDuty(waiter.getId())).thenReturn(true);
        when(sessions.findByIdForUpdate(31L)).thenReturn(Optional.of(session));

        var result = service.claim(31L, waiter.getEmail());

        assertEquals(waiter.getId(), result.staffId());
        assertEquals(waiter.getFullName(), result.staffName());
        assertEquals(waiter.getEmail(), result.assignedBy());
    }

    @Test
    void waiterCannotTakeAnotherWaitersSession() {
        AppUser first = user(7L, "Nguyễn Minh Anh", "staff@khamphaviet.vn", UserRole.STAFF);
        AppUser second = user(8L, "Hoàng Văn Nam", "staff2@khamphaviet.vn", UserRole.STAFF);
        ServiceSession session = session(31L);
        session.assignStaff(first.getId(), first.getFullName(), first.getEmail(), "admin@khamphaviet.vn");
        when(users.findByEmailIgnoreCase(second.getEmail())).thenReturn(Optional.of(second));
        when(shifts.isOnDuty(second.getId())).thenReturn(true);
        when(sessions.findByIdForUpdate(31L)).thenReturn(Optional.of(session));

        assertThrows(ConflictException.class, () -> service.claim(31L, second.getEmail()));
    }

    @Test
    void adminCanReassignSessionToAnotherWaiter() {
        AppUser admin = user(1L, "Quản trị viên", "admin@khamphaviet.vn", UserRole.ADMIN);
        AppUser waiter = user(8L, "Hoàng Văn Nam", "staff2@khamphaviet.vn", UserRole.STAFF);
        ServiceSession session = session(31L);
        when(users.findByEmailIgnoreCase(admin.getEmail())).thenReturn(Optional.of(admin));
        when(users.findById(waiter.getId())).thenReturn(Optional.of(waiter));
        when(shifts.isOnDuty(waiter.getId())).thenReturn(true);
        when(sessions.findByIdForUpdate(31L)).thenReturn(Optional.of(session));

        var result = service.assign(31L, waiter.getId(), admin.getEmail());

        assertEquals(waiter.getId(), result.staffId());
        assertEquals(admin.getEmail(), result.assignedBy());
    }

    @Test
    void waiterCannotClaimTableOutsideAnActiveShift() {
        AppUser waiter = user(7L, "Nguyễn Minh Anh", "staff@khamphaviet.vn", UserRole.STAFF);
        when(users.findByEmailIgnoreCase(waiter.getEmail())).thenReturn(Optional.of(waiter));
        when(shifts.isOnDuty(waiter.getId())).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.claim(31L, waiter.getEmail()));
    }

    @Test
    void transferRequiresAReason() {
        AppUser admin = user(1L, "Quản trị viên", "admin@khamphaviet.vn", UserRole.ADMIN);
        AppUser first = user(7L, "Nguyễn Minh Anh", "staff@khamphaviet.vn", UserRole.STAFF);
        AppUser second = user(8L, "Hoàng Văn Nam", "staff2@khamphaviet.vn", UserRole.STAFF);
        ServiceSession session = session(31L);
        session.assignStaff(first.getId(), first.getFullName(), first.getEmail(), admin.getEmail());
        when(users.findByEmailIgnoreCase(admin.getEmail())).thenReturn(Optional.of(admin));
        when(users.findById(second.getId())).thenReturn(Optional.of(second));
        when(shifts.isOnDuty(second.getId())).thenReturn(true);
        when(sessions.findByIdForUpdate(31L)).thenReturn(Optional.of(session));

        assertThrows(BusinessException.class,
                () -> service.assign(31L, second.getId(), admin.getEmail(), " "));
    }

    private AppUser user(Long id, String name, String email, UserRole role) {
        AppUser user = new AppUser(name, email, "hash", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private ServiceSession session(Long id) {
        ServiceSession session = new ServiceSession(99L);
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }
}
