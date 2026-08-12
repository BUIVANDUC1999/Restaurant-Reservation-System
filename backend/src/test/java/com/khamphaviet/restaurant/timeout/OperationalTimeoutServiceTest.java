package com.khamphaviet.restaurant.timeout;

import com.khamphaviet.restaurant.deposit.ReservationDepositRepository;
import com.khamphaviet.restaurant.notification.NotificationService;
import com.khamphaviet.restaurant.notification.NotificationType;
import com.khamphaviet.restaurant.order.*;
import com.khamphaviet.restaurant.reservation.*;
import com.khamphaviet.restaurant.service.*;
import com.khamphaviet.restaurant.table.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OperationalTimeoutServiceTest {
    private OperationalTimeoutRepository timeouts;
    private DiningOrderItemRepository orderItems;
    private DiningOrderRepository orders;
    private ServiceSessionRepository sessions;
    private ReservationTableAssignmentRepository assignments;
    private RestaurantTableRepository tables;
    private NotificationService notifications;
    private OperationalTimeoutService service;

    @BeforeEach
    void setUp() {
        timeouts = mock(OperationalTimeoutRepository.class);
        orderItems = mock(DiningOrderItemRepository.class);
        orders = mock(DiningOrderRepository.class);
        sessions = mock(ServiceSessionRepository.class);
        assignments = mock(ReservationTableAssignmentRepository.class);
        tables = mock(RestaurantTableRepository.class);
        notifications = mock(NotificationService.class);

        OperationalTimePolicy policy = new OperationalTimePolicy();
        service = new OperationalTimeoutService(timeouts, mock(OperationalTimeoutEventRepository.class), policy,
                mock(ReservationRepository.class), mock(ReservationDepositRepository.class), assignments,
                orders, orderItems, sessions, mock(TableServiceRequestRepository.class), tables, notifications);
        when(timeouts.findByDedupeKey(anyString())).thenReturn(Optional.empty());
        when(timeouts.findByTypeAndEntityTypeAndEntityIdAndStatus(any(), anyString(), anyLong(), any()))
                .thenReturn(List.of());
        when(timeouts.save(any())).thenAnswer(invocation -> {
            OperationalTimeout timeout = invocation.getArgument(0);
            ReflectionTestUtils.setField(timeout, "id", 700L);
            return timeout;
        });
    }

    @Test
    void warnsKitchenBeforeTheEstimatedReadyTime() {
        DiningOrderItem item = kitchenItem(Instant.now().plusSeconds(120));
        stubActiveItems(item);
        stubContext(item);

        service.monitor();

        verify(notifications).createStaffAlert(eq(99L), eq(NotificationType.KITCHEN_DELAY),
                contains("Sắp trễ"), contains("còn 2p"), startsWith("kitchen-prewarning-"));
        verify(timeouts, never()).save(any());
    }

    @Test
    void escalatesAnOverdueDishToTheAssignedWaiterWithTableContext() {
        DiningOrderItem item = kitchenItem(Instant.now().minusSeconds(6 * 60L));
        stubActiveItems(item);
        stubContext(item);

        service.monitor();

        ArgumentCaptor<OperationalTimeout> captor = ArgumentCaptor.forClass(OperationalTimeout.class);
        verify(timeouts).save(captor.capture());
        OperationalTimeout timeout = captor.getValue();
        assertEquals(99L, timeout.getReservationId());
        assertEquals(5L, timeout.getTableId());
        assertEquals("Nguyễn Minh Anh", timeout.getAssignedTo());
        assertTrue(timeout.getTitle().contains("B07"));
        verify(notifications).createStaffAlert(eq(99L), eq(NotificationType.KITCHEN_DELAY),
                contains("Món chậm"), contains("Nguyễn Minh Anh báo khách"), endsWith("-waiter"));
    }

    private DiningOrderItem kitchenItem(Instant eta) {
        DiningOrderItem item = new DiningOrderItem(55L, 3L, "Cá hồi nướng", BigDecimal.TEN, 2, 8);
        ReflectionTestUtils.setField(item, "id", 88L);
        ReflectionTestUtils.setField(item, "estimatedReadyAt", eta);
        return item;
    }

    private void stubActiveItems(DiningOrderItem item) {
        when(orderItems.findByStatusIn(anyList())).thenAnswer(invocation -> {
            List<?> statuses = invocation.getArgument(0);
            return statuses.contains(DiningOrderItemStatus.SUBMITTED) ? List.of(item) : List.of();
        });
    }

    private void stubContext(DiningOrderItem item) {
        DiningOrder order = new DiningOrder(22L, null);
        ReflectionTestUtils.setField(order, "id", item.getOrderId());
        ServiceSession session = new ServiceSession(99L);
        ReflectionTestUtils.setField(session, "id", 22L);
        session.assignStaff(7L, "Nguyễn Minh Anh", "staff@khamphaviet.vn", "admin");
        ReservationTableAssignment assignment = new ReservationTableAssignment(99L, 5L);
        RestaurantTable table = new RestaurantTable("B07", "Bàn trung tâm 1", "Tầng 1", "Trung tâm", 4);
        ReflectionTestUtils.setField(table, "id", 5L);

        when(orders.findById(item.getOrderId())).thenReturn(Optional.of(order));
        when(sessions.findById(order.getServiceSessionId())).thenReturn(Optional.of(session));
        when(assignments.findByReservationId(99L)).thenReturn(List.of(assignment));
        when(tables.findAllById(anyList())).thenReturn(List.of(table));
    }
}
