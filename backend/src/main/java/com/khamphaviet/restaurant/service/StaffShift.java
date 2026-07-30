package com.khamphaviet.restaurant.service;

import com.khamphaviet.restaurant.auth.AppUser;
import com.khamphaviet.restaurant.common.BusinessException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "staff_shifts")
@Getter
@NoArgsConstructor
public class StaffShift {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @Column(nullable = false) private Long staffId;
    @Column(nullable = false, length = 120) private String staffName;
    @Column(nullable = false, length = 180) private String staffEmail;
    @Column(nullable = false) private Instant startsAt;
    @Column(nullable = false) private Instant endsAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StaffShiftStatus status;
    private Instant actualStartedAt;
    private Instant actualEndedAt;
    @Column(nullable = false, length = 180) private String createdBy;
    @Column(nullable = false) private Instant createdAt;

    public StaffShift(AppUser staff, Instant startsAt, Instant endsAt, String createdBy) {
        if (!endsAt.isAfter(startsAt)) throw new BusinessException("Giờ kết thúc ca phải sau giờ bắt đầu");
        this.staffId = staff.getId();
        this.staffName = staff.getFullName();
        this.staffEmail = staff.getEmail();
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = StaffShiftStatus.SCHEDULED;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public void start() {
        if (status != StaffShiftStatus.SCHEDULED)
            throw new BusinessException("Chỉ ca đang lên lịch mới có thể bắt đầu");
        status = StaffShiftStatus.ACTIVE;
        actualStartedAt = Instant.now();
    }

    public void complete() {
        if (status != StaffShiftStatus.ACTIVE)
            throw new BusinessException("Chỉ ca đang hoạt động mới có thể kết thúc");
        status = StaffShiftStatus.COMPLETED;
        actualEndedAt = Instant.now();
    }

    public void cancel() {
        if (status == StaffShiftStatus.ACTIVE || status == StaffShiftStatus.COMPLETED)
            throw new BusinessException("Không thể hủy ca đã bắt đầu");
        status = StaffShiftStatus.CANCELLED;
    }
}
