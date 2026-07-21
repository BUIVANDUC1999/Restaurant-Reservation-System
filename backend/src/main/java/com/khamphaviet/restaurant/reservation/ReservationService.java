package com.khamphaviet.restaurant.reservation;

import com.khamphaviet.restaurant.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationService {
    private static final int TOTAL_CAPACITY = 300;
    private static final List<ReservationStatus> OCCUPYING = List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN);
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final ReservationRepository repository;
    private final SecureRandom random = new SecureRandom();

    public ReservationService(ReservationRepository repository) { this.repository = repository; }

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
    public Reservation create(ReservationDtos.CreateRequest request) {
        var available = availability(request.reservationDate(), request.timeSlot(), request.partySize());
        if (!available.available()) throw new BusinessException("Không còn đủ chỗ cho số khách đã chọn");
        return repository.save(new Reservation(nextCode(), request.customerName().trim(), request.phone().trim(),
                request.email(), request.reservationDate(), request.timeSlot(), request.partySize(),
                request.preferredFloor(), request.note()));
    }

    public Reservation lookup(String code, String phone) {
        return repository.findByCodeIgnoreCaseAndPhone(code.trim(), phone.trim())
                .orElseThrow(() -> new BusinessException("Không tìm thấy đơn đặt bàn"));
    }

    public List<Reservation> list() { return repository.findAllByOrderByReservationDateDescCreatedAtDesc(); }

    @Transactional
    public Reservation updateStatus(Long id, ReservationStatus status) {
        Reservation reservation = repository.findById(id).orElseThrow(() -> new BusinessException("Không tìm thấy đơn đặt bàn"));
        reservation.changeStatus(status);
        return reservation;
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

