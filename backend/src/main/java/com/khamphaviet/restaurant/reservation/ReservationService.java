package com.khamphaviet.restaurant.reservation;

import com.khamphaviet.restaurant.common.BusinessException;
import com.khamphaviet.restaurant.menu.MenuItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class ReservationService {
    private static final int TOTAL_CAPACITY = 300;
    private static final List<ReservationStatus> OCCUPYING = List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN);
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final ReservationRepository repository;
    private final ReservationItemRepository itemRepository;
    private final MenuItemRepository menuRepository;
    private final SecureRandom random = new SecureRandom();

    public ReservationService(ReservationRepository repository, ReservationItemRepository itemRepository, MenuItemRepository menuRepository) {
        this.repository = repository; this.itemRepository = itemRepository; this.menuRepository = menuRepository;
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

    @Transactional
    public ReservationDtos.ReservationResponse create(ReservationDtos.CreateRequest request) {
        var available = availability(request.reservationDate(), request.timeSlot(), request.partySize());
        if (!available.available()) throw new BusinessException("Không còn đủ chỗ cho số khách đã chọn");
        Reservation reservation = repository.save(new Reservation(nextCode(), request.customerName().trim(), request.phone().trim(),
                request.email(), request.reservationDate(), request.timeSlot(), request.partySize(),
                request.preferredFloor(), request.note()));
        savePreOrderItems(reservation.getId(), request.preOrderItems());
        return response(reservation, itemRepository.findByReservationIdOrderByIdAsc(reservation.getId()));
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
    public Reservation updateStatus(Long id, ReservationStatus status) {
        Reservation reservation = repository.findById(id).orElseThrow(() -> new BusinessException("Không tìm thấy đơn đặt bàn"));
        reservation.changeStatus(status);
        return reservation;
    }

    @Transactional
    public ReservationDtos.ReservationResponse confirmPreOrder(Long id) {
        Reservation reservation = repository.findById(id).orElseThrow(() -> new BusinessException("Không tìm thấy đơn đặt bàn"));
        List<ReservationItem> items = itemRepository.findByReservationIdOrderByIdAsc(id);
        if (items.isEmpty()) throw new BusinessException("Đơn đặt bàn chưa chọn món trước");
        items.forEach(ReservationItem::confirm);
        return response(reservation, items);
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

    private ReservationDtos.ReservationResponse response(Reservation reservation, List<ReservationItem> items) {
        var itemResponses = items.stream().map(item -> new ReservationDtos.PreOrderItemResponse(item.getId(), item.getMenuItemId(),
                item.getItemNameSnapshot(), item.getUnitPrice(), item.getQuantity(), item.getStatus(),
                item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))).toList();
        return new ReservationDtos.ReservationResponse(reservation.getId(), reservation.getCode(), reservation.getCustomerName(),
                reservation.getPhone(), reservation.getEmail(), reservation.getReservationDate(), reservation.getTimeSlot(),
                reservation.getPartySize(), reservation.getPreferredFloor(), reservation.getNote(), reservation.getStatus(),
                reservation.getCreatedAt(), itemResponses);
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
