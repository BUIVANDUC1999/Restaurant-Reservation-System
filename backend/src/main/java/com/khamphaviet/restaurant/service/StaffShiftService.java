package com.khamphaviet.restaurant.service;

import com.khamphaviet.restaurant.auth.AppUser;
import com.khamphaviet.restaurant.auth.AppUserRepository;
import com.khamphaviet.restaurant.auth.UserRole;
import com.khamphaviet.restaurant.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class StaffShiftService {
    private static final ZoneId RESTAURANT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<StaffShiftStatus> BLOCKING =
            List.of(StaffShiftStatus.SCHEDULED, StaffShiftStatus.ACTIVE);
    private final StaffShiftRepository shifts;
    private final AppUserRepository users;
    private final ServiceSessionRepository sessions;

    public StaffShiftService(StaffShiftRepository shifts, AppUserRepository users, ServiceSessionRepository sessions) {
        this.shifts = shifts;
        this.users = users;
        this.sessions = sessions;
    }

    public record ShiftResponse(Long id, Long staffId, String staffName, String staffEmail,
                                Instant startsAt, Instant endsAt, StaffShiftStatus status,
                                Instant actualStartedAt, Instant actualEndedAt,
                                String createdBy, Instant createdAt, boolean onDuty) {}

    public List<ShiftResponse> today() {
        LocalDate today = LocalDate.now(RESTAURANT_ZONE);
        Instant start = today.atStartOfDay(RESTAURANT_ZONE).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(RESTAURANT_ZONE).toInstant();
        return shifts.findByStartsAtLessThanAndEndsAtGreaterThanOrderByStartsAtAsc(end, start)
                .stream().map(this::response).toList();
    }

    @Transactional
    public ShiftResponse create(Long staffId, Instant startsAt, Instant endsAt, String actor) {
        AppUser staff = users.findById(staffId)
                .filter(AppUser::isActive)
                .filter(user -> user.getRole() == UserRole.STAFF)
                .orElseThrow(() -> new BusinessException("Nhân viên phục vụ không hợp lệ hoặc đã bị khóa"));
        if (!endsAt.isAfter(startsAt)) throw new BusinessException("Giờ kết thúc ca phải sau giờ bắt đầu");
        long minutes = Duration.between(startsAt, endsAt).toMinutes();
        if (minutes < 60 || minutes > 16 * 60)
            throw new BusinessException("Ca làm việc phải kéo dài từ 1 đến 16 giờ");
        boolean overlaps = shifts.findByStaffIdAndStatusIn(staffId, BLOCKING).stream()
                .anyMatch(shift -> startsAt.isBefore(shift.getEndsAt()) && endsAt.isAfter(shift.getStartsAt()));
        if (overlaps) throw new BusinessException("Nhân viên đã có ca làm việc trùng thời gian");
        return response(shifts.save(new StaffShift(staff, startsAt, endsAt, actor)));
    }

    @Transactional
    public ShiftResponse start(Long id) {
        StaffShift shift = shift(id);
        if (Instant.now().isAfter(shift.getEndsAt()))
            throw new BusinessException("Ca đã qua giờ kết thúc, không thể bắt đầu");
        shift.start();
        return response(shift);
    }

    @Transactional
    public ShiftResponse complete(Long id) {
        StaffShift shift = shift(id);
        long activeTables = sessions.countByAssignedStaffIdAndStatus(shift.getStaffId(), ServiceSessionStatus.ACTIVE);
        if (activeTables > 0)
            throw new BusinessException("Nhân viên còn " + activeTables
                    + " lượt khách đang phụ trách. Cần bàn giao trước khi kết thúc ca");
        shift.complete();
        return response(shift);
    }

    @Transactional
    public ShiftResponse cancel(Long id) {
        StaffShift shift = shift(id);
        shift.cancel();
        return response(shift);
    }

    public List<StaffShift> onDutyNow() {
        Instant now = Instant.now();
        return shifts.findByStatusAndStartsAtLessThanEqualAndEndsAtGreaterThanOrderByStartsAtAsc(
                StaffShiftStatus.ACTIVE, now, now);
    }

    public boolean isOnDuty(Long staffId) {
        return onDutyNow().stream().anyMatch(shift -> shift.getStaffId().equals(staffId));
    }

    private StaffShift shift(Long id) {
        return shifts.findById(id).orElseThrow(() -> new BusinessException("Không tìm thấy ca làm việc"));
    }

    private ShiftResponse response(StaffShift shift) {
        Instant now = Instant.now();
        boolean onDuty = shift.getStatus() == StaffShiftStatus.ACTIVE
                && !now.isBefore(shift.getStartsAt()) && now.isBefore(shift.getEndsAt());
        return new ShiftResponse(shift.getId(), shift.getStaffId(), shift.getStaffName(), shift.getStaffEmail(),
                shift.getStartsAt(), shift.getEndsAt(), shift.getStatus(), shift.getActualStartedAt(),
                shift.getActualEndedAt(), shift.getCreatedBy(), shift.getCreatedAt(), onDuty);
    }
}
