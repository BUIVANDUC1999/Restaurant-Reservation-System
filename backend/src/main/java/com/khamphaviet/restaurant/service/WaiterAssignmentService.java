package com.khamphaviet.restaurant.service;

import com.khamphaviet.restaurant.auth.AppUser;
import com.khamphaviet.restaurant.auth.AppUserRepository;
import com.khamphaviet.restaurant.auth.UserRole;
import com.khamphaviet.restaurant.common.BusinessException;
import com.khamphaviet.restaurant.common.ConflictException;
import com.khamphaviet.restaurant.reservation.Reservation;
import com.khamphaviet.restaurant.reservation.ReservationRepository;
import com.khamphaviet.restaurant.reservation.ReservationTableAssignment;
import com.khamphaviet.restaurant.reservation.ReservationTableAssignmentRepository;
import com.khamphaviet.restaurant.table.RestaurantTable;
import com.khamphaviet.restaurant.table.RestaurantTableRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class WaiterAssignmentService {
    private final ServiceSessionRepository sessions;
    private final AppUserRepository users;
    private final StaffShiftService shifts;
    private final WaiterAssignmentEventRepository events;
    private final ReservationRepository reservations;
    private final ReservationTableAssignmentRepository tableAssignments;
    private final RestaurantTableRepository tables;

    public WaiterAssignmentService(ServiceSessionRepository sessions, AppUserRepository users,
            StaffShiftService shifts, WaiterAssignmentEventRepository events,
            ReservationRepository reservations, ReservationTableAssignmentRepository tableAssignments,
            RestaurantTableRepository tables) {
        this.sessions = sessions;
        this.users = users;
        this.shifts = shifts;
        this.events = events;
        this.reservations = reservations;
        this.tableAssignments = tableAssignments;
        this.tables = tables;
    }

    public record WaiterSummary(Long id, String fullName, String email, Long shiftId, Instant shiftEndsAt,
                                int sessionCount, int tableCount, int guestCount, List<String> tableCodes,
                                String loadLevel, boolean recommended) {}
    public record Assignment(Long serviceSessionId, Long staffId, String staffName, String staffEmail,
                             String assignedBy, Instant assignedAt) {}
    public record AssignmentEventResponse(Long id, Long serviceSessionId, Long reservationId,
                                          WaiterAssignmentAction action, Long fromStaffId, String fromStaffName,
                                          Long toStaffId, String toStaffName, String actor, String reason,
                                          Instant createdAt) {}
    private record Workload(StaffShift shift, int sessions, int tables, int guests,
                            List<String> tableCodes, int score) {}

    public List<WaiterSummary> waiters() {
        List<StaffShift> onDuty = shifts.onDutyNow();
        if (onDuty.isEmpty()) return List.of();
        Set<Long> staffIds = onDuty.stream().map(StaffShift::getStaffId).collect(java.util.stream.Collectors.toSet());
        List<ServiceSession> active = sessions.findByStatus(ServiceSessionStatus.ACTIVE).stream()
                .filter(session -> session.getAssignedStaffId() != null && staffIds.contains(session.getAssignedStaffId()))
                .toList();
        List<Long> reservationIds = active.stream().map(ServiceSession::getReservationId).distinct().toList();
        Map<Long, Reservation> reservationMap = new HashMap<>();
        reservations.findAllById(reservationIds).forEach(value -> reservationMap.put(value.getId(), value));
        Map<Long, List<Long>> tableIdsByReservation = new HashMap<>();
        if (!reservationIds.isEmpty()) {
            tableAssignments.findByReservationIdIn(reservationIds).forEach(assignment ->
                    tableIdsByReservation.computeIfAbsent(assignment.getReservationId(), ignored -> new ArrayList<>())
                            .add(assignment.getTableId()));
        }
        Set<Long> tableIds = tableIdsByReservation.values().stream().flatMap(Collection::stream)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, String> tableCodes = new HashMap<>();
        tables.findAllById(tableIds).forEach(table -> tableCodes.put(table.getId(), table.getCode()));

        List<Workload> workloads = onDuty.stream().map(shift -> {
            List<ServiceSession> mine = active.stream()
                    .filter(session -> shift.getStaffId().equals(session.getAssignedStaffId())).toList();
            List<Long> mineReservations = mine.stream().map(ServiceSession::getReservationId).toList();
            List<String> codes = mineReservations.stream()
                    .flatMap(id -> tableIdsByReservation.getOrDefault(id, List.of()).stream())
                    .map(tableCodes::get).filter(Objects::nonNull).distinct().sorted().toList();
            int guests = mineReservations.stream().map(reservationMap::get).filter(Objects::nonNull)
                    .mapToInt(Reservation::getPartySize).sum();
            int score = mine.size() * 5 + codes.size() * 3 + guests;
            return new Workload(shift, mine.size(), codes.size(), guests, codes, score);
        }).toList();
        Long recommendedId = workloads.stream()
                .min(Comparator.comparingInt(Workload::score)
                        .thenComparing(workload -> workload.shift().getStaffId()))
                .map(workload -> workload.shift().getStaffId()).orElse(null);
        return workloads.stream().map(workload -> new WaiterSummary(
                workload.shift().getStaffId(), workload.shift().getStaffName(), workload.shift().getStaffEmail(),
                workload.shift().getId(), workload.shift().getEndsAt(), workload.sessions(), workload.tables(),
                workload.guests(), workload.tableCodes(), loadLevel(workload.tables(), workload.guests()),
                workload.shift().getStaffId().equals(recommendedId))).toList();
    }

    @Transactional
    public Assignment claim(Long sessionId, String actorEmail) {
        return claim(sessionId, actorEmail, "Nhân viên tự nhận bàn");
    }

    @Transactional
    public Assignment claim(Long sessionId, String actorEmail, String reason) {
        AppUser actor = actor(actorEmail);
        if (actor.getRole() != UserRole.STAFF)
            throw new AccessDeniedException("Chỉ nhân viên phục vụ được tự nhận bàn");
        if (!shifts.isOnDuty(actor.getId()))
            throw new BusinessException("Bạn chưa có ca làm việc đang hoạt động");
        ServiceSession session = activeSession(sessionId);
        if (session.getAssignedStaffId() != null && !session.getAssignedStaffId().equals(actor.getId()))
            throw new ConflictException("Bàn đã được phân cho " + session.getAssignedStaffName());
        if (actor.getId().equals(session.getAssignedStaffId())) return response(session);
        events.save(new WaiterAssignmentEvent(session, WaiterAssignmentAction.CLAIM,
                null, null, actor.getId(), actor.getFullName(), actor.getEmail(),
                text(reason, "Nhân viên tự nhận bàn", false)));
        session.assignStaff(actor.getId(), actor.getFullName(), actor.getEmail(), actor.getEmail());
        return response(session);
    }

    @Transactional
    public Assignment assign(Long sessionId, Long staffId, String actorEmail) {
        return assign(sessionId, staffId, actorEmail, null);
    }

    @Transactional
    public Assignment assign(Long sessionId, Long staffId, String actorEmail, String reason) {
        AppUser actor = actor(actorEmail);
        requireAdmin(actor);
        AppUser waiter = users.findById(staffId)
                .filter(AppUser::isActive)
                .filter(user -> user.getRole() == UserRole.STAFF)
                .orElseThrow(() -> new BusinessException("Nhân viên phục vụ không hợp lệ hoặc đã bị khóa"));
        if (!shifts.isOnDuty(waiter.getId()))
            throw new BusinessException("Nhân viên chưa vào ca hoặc ca làm việc đã kết thúc");
        ServiceSession session = activeSession(sessionId);
        if (waiter.getId().equals(session.getAssignedStaffId())) return response(session);
        boolean transfer = session.getAssignedStaffId() != null;
        String auditReason = text(reason, transfer ? null : "Admin phân công bàn", transfer);
        events.save(new WaiterAssignmentEvent(session,
                transfer ? WaiterAssignmentAction.TRANSFER : WaiterAssignmentAction.ASSIGN,
                session.getAssignedStaffId(), session.getAssignedStaffName(),
                waiter.getId(), waiter.getFullName(), actor.getEmail(), auditReason));
        session.assignStaff(waiter.getId(), waiter.getFullName(), waiter.getEmail(), actor.getEmail());
        return response(session);
    }

    @Transactional
    public Assignment unassign(Long sessionId, String actorEmail) {
        return unassign(sessionId, actorEmail, null);
    }

    @Transactional
    public Assignment unassign(Long sessionId, String actorEmail, String reason) {
        AppUser actor = actor(actorEmail);
        requireAdmin(actor);
        ServiceSession session = activeSession(sessionId);
        if (session.getAssignedStaffId() == null) return response(session);
        events.save(new WaiterAssignmentEvent(session, WaiterAssignmentAction.UNASSIGN,
                session.getAssignedStaffId(), session.getAssignedStaffName(), null, null, actor.getEmail(),
                text(reason, null, true)));
        session.clearStaff(actor.getEmail());
        return response(session);
    }

    public List<AssignmentEventResponse> history(Long sessionId) {
        List<WaiterAssignmentEvent> history = sessionId == null
                ? events.findTop50ByOrderByCreatedAtDesc()
                : events.findByServiceSessionIdOrderByCreatedAtDesc(sessionId);
        return history.stream().map(this::eventResponse).toList();
    }

    private AppUser actor(String email) {
        return users.findByEmailIgnoreCase(email)
                .filter(AppUser::isActive)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản không còn hoạt động"));
    }

    private ServiceSession activeSession(Long id) {
        ServiceSession session = sessions.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phiên phục vụ"));
        if (session.getStatus() != ServiceSessionStatus.ACTIVE)
            throw new BusinessException("Phiên phục vụ đã kết thúc");
        return session;
    }

    private void requireAdmin(AppUser actor) {
        if (actor.getRole() != UserRole.ADMIN)
            throw new AccessDeniedException("Chỉ quản trị viên được điều phối nhân viên");
    }

    private String text(String value, String fallback, boolean required) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) normalized = fallback == null ? "" : fallback;
        if (required && normalized.isBlank()) throw new BusinessException("Cần nhập lý do bàn giao");
        if (normalized.length() > 400) throw new BusinessException("Lý do bàn giao tối đa 400 ký tự");
        return normalized;
    }

    private String loadLevel(int tableCount, int guestCount) {
        if (tableCount > 5 || guestCount > 28) return "OVERLOADED";
        if (tableCount > 3 || guestCount > 16) return "BUSY";
        return "NORMAL";
    }

    private Assignment response(ServiceSession session) {
        return new Assignment(session.getId(), session.getAssignedStaffId(), session.getAssignedStaffName(),
                session.getAssignedStaffEmail(), session.getAssignedBy(), session.getAssignedAt());
    }

    private AssignmentEventResponse eventResponse(WaiterAssignmentEvent event) {
        return new AssignmentEventResponse(event.getId(), event.getServiceSessionId(), event.getReservationId(),
                event.getAction(), event.getFromStaffId(), event.getFromStaffName(), event.getToStaffId(),
                event.getToStaffName(), event.getActor(), event.getReason(), event.getCreatedAt());
    }
}
