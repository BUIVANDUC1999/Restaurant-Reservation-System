package com.khamphaviet.restaurant.reservation;

import com.khamphaviet.restaurant.common.BusinessException;
import com.khamphaviet.restaurant.menu.MenuItemRepository;
import com.khamphaviet.restaurant.table.*;
import com.khamphaviet.restaurant.service.*;
import com.khamphaviet.restaurant.order.*;
import com.khamphaviet.restaurant.billing.*;
import com.khamphaviet.restaurant.deposit.*;
import com.khamphaviet.restaurant.notification.NotificationService;
import com.khamphaviet.restaurant.timeout.OperationalTimePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;

@Service
public class ReservationService {
    private static final int TOTAL_CAPACITY = 300;
    private static final List<ReservationStatus> OCCUPYING = List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN);
    private static final List<DiningOrderStatus> OPEN_ORDERS = List.of(DiningOrderStatus.SUBMITTED, DiningOrderStatus.PREPARING, DiningOrderStatus.READY);
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final ReservationRepository repository;
    private final ReservationItemRepository itemRepository;
    private final MenuItemRepository menuRepository;
    private final RestaurantTableRepository tableRepository;
    private final ReservationTableAssignmentRepository assignmentRepository;
    private final ServiceSessionRepository sessionRepository;
    private final DiningOrderRepository diningOrderRepository;
    private final DiningOrderService diningOrderService;
    private final PaymentRepository paymentRepository;
    private final ReservationDepositRepository depositRepository;
    private final TableSchedulingService schedulingService;
    private final NotificationService notificationService;
    private final OperationalTimePolicy timePolicy;
    private final SecureRandom random = new SecureRandom();

    public ReservationService(ReservationRepository repository, ReservationItemRepository itemRepository, MenuItemRepository menuRepository,
                              RestaurantTableRepository tableRepository, ReservationTableAssignmentRepository assignmentRepository,
                              ServiceSessionRepository sessionRepository, DiningOrderRepository diningOrderRepository,
                              DiningOrderService diningOrderService, PaymentRepository paymentRepository,
                              ReservationDepositRepository depositRepository, TableSchedulingService schedulingService,
                              NotificationService notificationService, OperationalTimePolicy timePolicy) {
        this.repository = repository; this.itemRepository = itemRepository; this.menuRepository = menuRepository;
        this.tableRepository = tableRepository; this.assignmentRepository = assignmentRepository; this.sessionRepository = sessionRepository;
        this.diningOrderRepository = diningOrderRepository;
        this.diningOrderService = diningOrderService;
        this.paymentRepository = paymentRepository;
        this.depositRepository = depositRepository;
        this.schedulingService = schedulingService;
        this.notificationService = notificationService;
        this.timePolicy = timePolicy;
    }

    public ReservationDtos.AvailabilityResponse availability(LocalDate date, String slot, int partySize) {
        validateSlot(slot);
        if (date.isBefore(LocalDate.now())) throw new BusinessException("Ngày đặt bàn không được ở trong quá khứ");
        int reserved = repository.findByReservationDateAndTimeSlotAndStatusIn(date, slot, OCCUPYING)
                .stream().mapToInt(Reservation::getPartySize).sum();
        int remaining = Math.max(0, TOTAL_CAPACITY - reserved);
        return new ReservationDtos.AvailabilityResponse(remaining >= partySize, remaining,
                remaining >= partySize ? "Còn chỗ phù hợp" : "Khung giờ này không đủ chỗ");
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ReservationDtos.ReservationResponse create(ReservationDtos.CreateRequest request) {
        LocalTime reservationTime=request.reservationTime()!=null?request.reservationTime():
                ("LUNCH".equals(request.timeSlot())?LocalTime.of(11,0):LocalTime.of(17,30));
        int duration=request.durationMinutes()==null?120:request.durationMinutes();
        schedulingService.validateSchedule(request.reservationDate(),reservationTime,duration);
        var available = availability(request.reservationDate(), request.timeSlot(), request.partySize());
        if (!available.available()) throw new BusinessException("Không còn đủ chỗ cho số khách đã chọn");
        List<RestaurantTable> selected=request.selectedTableIds()==null||request.selectedTableIds().isEmpty()?List.of():
                schedulingService.validateSelection(request.reservationDate(),reservationTime,duration,request.partySize(),request.selectedTableIds(),null);
        Reservation reservation = repository.save(new Reservation(nextCode(), request.customerName().trim(), request.phone().trim(),
                request.email(), request.reservationDate(), request.timeSlot(), reservationTime, duration, request.partySize(),
                "GROUND_FLOOR", request.note(), Boolean.TRUE.equals(request.notifyEmail()), !Boolean.FALSE.equals(request.notifySms()),
                timePolicy.getReservationHoldMinutes()));
        if(!selected.isEmpty()) assignmentRepository.saveAll(selected.stream()
                .map(table->new ReservationTableAssignment(reservation.getId(),table.getId())).toList());
        savePreOrderItems(reservation.getId(), request.preOrderItems());
        List<ReservationItem> savedItems=itemRepository.findByReservationIdOrderByIdAsc(reservation.getId());
        java.math.BigDecimal depositAmount=savedItems.isEmpty()
                ? java.math.BigDecimal.valueOf(200000L).multiply(java.math.BigDecimal.valueOf(request.partySize()))
                : savedItems.stream().map(item->item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
                    .reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add).multiply(new java.math.BigDecimal("0.10")).setScale(0,java.math.RoundingMode.HALF_UP);
        depositRepository.save(new ReservationDeposit(reservation.getId(),depositAmount));
        notificationService.reservationCreated(reservation);
        return response(reservation, savedItems);
    }

    public ReservationDtos.ReservationResponse lookup(String code, String phone) {
        Reservation reservation = repository.findByCodeIgnoreCaseAndPhone(code.trim(), phone.trim())
                .orElseThrow(() -> new BusinessException("Không tìm thấy đơn đặt bàn"));
        return response(reservation, itemRepository.findByReservationIdOrderByIdAsc(reservation.getId()));
    }

    public List<ReservationDtos.ReservationResponse> list() {
        List<Reservation> reservations = repository.findAllByOrderByReservationDateDescCreatedAtDesc();
        if (reservations.isEmpty()) return List.of();
        Map<Long, List<ReservationItem>> grouped = new HashMap<>();
        itemRepository.findByReservationIdInOrderByIdAsc(reservations.stream().map(Reservation::getId).toList())
                .forEach(item -> grouped.computeIfAbsent(item.getReservationId(), key -> new java.util.ArrayList<>()).add(item));
        return reservations.stream().map(r -> response(r, grouped.getOrDefault(r.getId(), List.of()))).toList();
    }

    @Transactional
    public ReservationDtos.ReservationResponse updateStatus(Long id, ReservationStatus status) {
        Reservation reservation = repository.findById(id).orElseThrow(() -> new BusinessException("Không tìm thấy đơn đặt bàn"));
        if (reservation.getStatus() != status) {
            boolean allowed = reservation.getStatus() == ReservationStatus.PENDING && List.of(ReservationStatus.CONFIRMED, ReservationStatus.CANCELLED, ReservationStatus.REJECTED).contains(status)
                    || reservation.getStatus() == ReservationStatus.CONFIRMED
                        && List.of(ReservationStatus.CANCELLED, ReservationStatus.NO_SHOW).contains(status);
            if (!allowed) throw new BusinessException("Chuyển trạng thái đặt bàn không hợp lệ");
        }
        if (List.of(ReservationStatus.CANCELLED, ReservationStatus.REJECTED, ReservationStatus.NO_SHOW).contains(status))
            releaseAssignedTables(id);
        reservation.changeStatus(status);
        return response(reservation, itemRepository.findByReservationIdOrderByIdAsc(id));
    }

    @Transactional
    public ReservationDtos.ReservationResponse confirmPreOrder(Long id) {
        Reservation reservation = repository.findById(id).orElseThrow(() -> new BusinessException("Không tìm thấy đơn đặt bàn"));
        List<ReservationItem> items = itemRepository.findByReservationIdOrderByIdAsc(id);
        if (items.isEmpty()) throw new BusinessException("Đơn đặt bàn chưa chọn món trước");
        items.forEach(ReservationItem::confirm);
        return response(reservation, items);
    }

    @Transactional
    public ReservationDtos.ReservationResponse assignTables(Long id, List<Long> requestedTableIds) {
        Reservation reservation = repository.findById(id).orElseThrow(() -> new BusinessException("Không tìm thấy đơn đặt bàn"));
        if (List.of(ReservationStatus.CHECKED_IN, ReservationStatus.COMPLETED, ReservationStatus.CANCELLED, ReservationStatus.REJECTED).contains(reservation.getStatus()))
            throw new BusinessException("Không thể đổi bàn ở trạng thái hiện tại");
        List<Long> tableIds = requestedTableIds.stream().distinct().toList();
        List<RestaurantTable> selectedTables = schedulingService.validateSelection(reservation.getReservationDate(),
                reservation.effectiveTime(),reservation.effectiveDurationMinutes(),reservation.getPartySize(),tableIds,id);
        List<ReservationTableAssignment> current = assignmentRepository.findByReservationId(id);
        var currentIds = current.stream().map(ReservationTableAssignment::getTableId).collect(java.util.stream.Collectors.toSet());

        var selectedIds = new HashSet<>(tableIds);
        List<ReservationTableAssignment> removed = current.stream().filter(a -> !selectedIds.contains(a.getTableId())).toList();
        if (!removed.isEmpty()) {
            assignmentRepository.deleteAll(removed);
        }
        var newAssignments = tableIds.stream().filter(tableId -> !currentIds.contains(tableId))
                .map(tableId -> new ReservationTableAssignment(id, tableId)).toList();
        assignmentRepository.saveAll(newAssignments);
        return response(reservation, itemRepository.findByReservationIdOrderByIdAsc(id));
    }

    @Transactional
    public ReservationDtos.ReservationResponse checkIn(Long id) {
        Reservation reservation = repository.findById(id).orElseThrow(() -> new BusinessException("Không tìm thấy đơn đặt bàn"));
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) throw new BusinessException("Đơn phải được xác nhận trước khi check-in");
        List<ReservationItem> preOrderItems = itemRepository.findByReservationIdOrderByIdAsc(id);
        if (preOrderItems.stream().anyMatch(item -> item.getStatus() == PreOrderStatus.REQUESTED))
            throw new BusinessException("Cần xác nhận món khách chọn trước khi check-in");
        List<ReservationTableAssignment> assignments = assignmentRepository.findByReservationId(id);
        if (assignments.isEmpty()) throw new BusinessException("Cần xếp bàn trước khi check-in");
        List<RestaurantTable> tables = tableRepository.findAllById(assignments.stream().map(ReservationTableAssignment::getTableId).toList());
        tables.forEach(table -> table.changeStatus(TableStatus.OCCUPIED));
        tableRepository.saveAll(tables);
        reservation.changeStatus(ReservationStatus.CHECKED_IN);
        ServiceSession session = sessionRepository.findByReservationId(id).orElseGet(() -> sessionRepository.save(new ServiceSession(id)));
        diningOrderService.createFromPreOrder(session.getId(), preOrderItems);
        return response(reservation, preOrderItems);
    }

    @Transactional
    public ReservationDtos.ReservationResponse completeService(Long id) {
        Reservation reservation = repository.findById(id).orElseThrow(() -> new BusinessException("Không tìm thấy đơn đặt bàn"));
        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) throw new BusinessException("Khách chưa check-in hoặc lượt phục vụ đã kết thúc");
        ServiceSession session = sessionRepository.findByReservationId(id).orElseThrow(() -> new BusinessException("Không tìm thấy phiên phục vụ"));
        long openOrderCount = diningOrderRepository.countByServiceSessionIdAndStatusIn(session.getId(), OPEN_ORDERS);
        if (openOrderCount > 0)
            throw new BusinessException("Còn " + openOrderCount + " phiếu món chưa phục vụ. Hãy xử lý xong trước khi hoàn tất lượt khách");
        if (!paymentRepository.existsByServiceSessionIdAndStatus(session.getId(), PaymentStatus.PAID))
            throw new BusinessException("Hóa đơn chưa được thanh toán");
        session.complete();
        List<ReservationTableAssignment> assignments = assignmentRepository.findByReservationId(id);
        List<RestaurantTable> tables = tableRepository.findAllById(assignments.stream().map(ReservationTableAssignment::getTableId).toList());
        tables.forEach(table -> table.changeStatus(TableStatus.NEEDS_CLEANING));
        tableRepository.saveAll(tables);
        reservation.changeStatus(ReservationStatus.COMPLETED);
        return response(reservation, itemRepository.findByReservationIdOrderByIdAsc(id));
    }

    private void savePreOrderItems(Long reservationId, List<ReservationDtos.PreOrderItemRequest> requests) {
        if (requests == null || requests.isEmpty()) return;
        if (requests.stream().map(ReservationDtos.PreOrderItemRequest::menuItemId).distinct().count() != requests.size())
            throw new BusinessException("Mỗi món chỉ được xuất hiện một lần");
        int totalQuantity = requests.stream().mapToInt(ReservationDtos.PreOrderItemRequest::quantity).sum();
        if (totalQuantity > 100) throw new BusinessException("Tổng số lượng món đặt trước không được vượt quá 100");
        requests.forEach(request -> {
            var menuItem = menuRepository.findById(request.menuItemId())
                    .filter(item -> item.isAvailable()).orElseThrow(() -> new BusinessException("Món đã chọn không còn phục vụ"));
            itemRepository.save(new ReservationItem(reservationId, menuItem.getId(), menuItem.getName(), menuItem.getPrice(), request.quantity()));
        });
    }

    private void releaseAssignedTables(Long reservationId) {
        List<ReservationTableAssignment> assignments = assignmentRepository.findByReservationId(reservationId);
        if (assignments.isEmpty()) return;
        List<RestaurantTable> tables = tableRepository.findAllById(assignments.stream().map(ReservationTableAssignment::getTableId).toList());
        tables.forEach(table -> table.changeStatus(TableStatus.AVAILABLE));
        tableRepository.saveAll(tables);
        assignmentRepository.deleteAll(assignments);
    }

    private ReservationDtos.ReservationResponse response(Reservation reservation, List<ReservationItem> items) {
        var itemResponses = items.stream().map(item -> new ReservationDtos.PreOrderItemResponse(item.getId(), item.getMenuItemId(),
                item.getItemNameSnapshot(), item.getUnitPrice(), item.getQuantity(), item.getStatus(),
                item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))).toList();
        List<RestaurantTable> assigned = tableRepository.findAllById(assignmentRepository.findByReservationId(reservation.getId()).stream()
                .map(ReservationTableAssignment::getTableId).toList());
        var assignedResponses = assigned.stream().map(table -> new ReservationDtos.AssignedTableResponse(table.getId(), table.getCode(),
                table.getName(), table.getFloor(), table.getArea(), table.getSeats(), table.getStatus(),
                table.getLayoutX(),table.getLayoutY(),table.getShape())).toList();
        Long sessionId = sessionRepository.findByReservationId(reservation.getId()).map(ServiceSession::getId).orElse(null);
        long openOrderCount = sessionId == null ? 0 : diningOrderRepository.countByServiceSessionIdAndStatusIn(sessionId, OPEN_ORDERS);
        boolean paid = sessionId != null && paymentRepository.existsByServiceSessionIdAndStatus(sessionId, PaymentStatus.PAID);
        ReservationDeposit deposit=depositRepository.findByReservationId(reservation.getId()).orElse(null);
        return new ReservationDtos.ReservationResponse(reservation.getId(), reservation.getCode(), reservation.getCustomerName(),
                reservation.getPhone(), reservation.getEmail(), reservation.getReservationDate(), reservation.getTimeSlot(),
                reservation.getPartySize(),reservation.effectiveTime(),reservation.effectiveDurationMinutes(),reservation.getHoldExpiresAt(),
                reservation.getPreferredFloor(), reservation.getNote(), reservation.getStatus(),
                reservation.getCreatedAt(),reservation.getConfirmedAt(),reservation.getCheckedInAt(),reservation.getCompletedAt(),
                reservation.isNotifyEmail(),reservation.isNotifySms(),itemResponses, assignedResponses, sessionId, openOrderCount, paid,
                deposit==null?java.math.BigDecimal.ZERO:deposit.getAmount(),deposit==null?DepositStatus.PENDING:deposit.getStatus(),
                deposit==null?null:deposit.getMethod(),deposit==null?null:deposit.getPaidAt());
    }

    private void validateSlot(String slot) {
        if (!"LUNCH".equals(slot) && !"DINNER".equals(slot)) throw new BusinessException("Ca phục vụ không hợp lệ");
    }
    private String nextCode() {
        StringBuilder code = new StringBuilder("KV-");
        for (int i = 0; i < 6; i++) code.append(CHARS.charAt(random.nextInt(CHARS.length())));
        return code.toString();
    }
}
