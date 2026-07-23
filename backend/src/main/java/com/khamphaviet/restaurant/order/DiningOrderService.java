package com.khamphaviet.restaurant.order;

import com.khamphaviet.restaurant.common.BusinessException;
import com.khamphaviet.restaurant.menu.MenuItemRepository;
import com.khamphaviet.restaurant.reservation.*;
import com.khamphaviet.restaurant.service.*;
import com.khamphaviet.restaurant.table.RestaurantTableRepository;
import com.khamphaviet.restaurant.notification.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
public class DiningOrderService {
    private final DiningOrderRepository orders; private final DiningOrderItemRepository items;
    private final ServiceSessionRepository sessions; private final MenuItemRepository menu;
    private final ReservationRepository reservations; private final ReservationTableAssignmentRepository assignments;
    private final RestaurantTableRepository tables;
    private final NotificationService notifications;

    public DiningOrderService(DiningOrderRepository orders, DiningOrderItemRepository items, ServiceSessionRepository sessions,
                              MenuItemRepository menu, ReservationRepository reservations,
                              ReservationTableAssignmentRepository assignments, RestaurantTableRepository tables,
                              NotificationService notifications) {
        this.orders=orders;this.items=items;this.sessions=sessions;this.menu=menu;this.reservations=reservations;
        this.assignments=assignments;this.tables=tables;
        this.notifications=notifications;
    }

    @Transactional
    public DiningOrderDtos.OrderResponse create(Long sessionId, DiningOrderDtos.CreateRequest request) {
        ServiceSession session=activeSession(sessionId);
        if(request.items().stream().map(DiningOrderDtos.ItemRequest::menuItemId).distinct().count()!=request.items().size())
            throw new BusinessException("Mỗi món chỉ được xuất hiện một lần trong phiếu gọi món");
        if(request.items().stream().mapToInt(DiningOrderDtos.ItemRequest::quantity).sum()>100)
            throw new BusinessException("Tổng số lượng món không được vượt quá 100");
        DiningOrder order=orders.save(new DiningOrder(sessionId,request.note()));
        request.items().forEach(line->{
            var dish=menu.findById(line.menuItemId()).filter(x->x.isAvailable())
                    .orElseThrow(()->new BusinessException("Một món đã chọn không còn phục vụ"));
            items.save(new DiningOrderItem(order.getId(),dish.getId(),dish.getName(),dish.getPrice(),line.quantity(),dish.getPreparationMinutes()));
        });
        return response(order,session);
    }

    public List<DiningOrderDtos.OrderResponse> listForSession(Long sessionId) {
        ServiceSession session=sessions.findById(sessionId).orElseThrow(()->new BusinessException("Không tìm thấy phiên phục vụ"));
        return orders.findByServiceSessionIdOrderByCreatedAtDesc(sessionId).stream().map(order->response(order,session)).toList();
    }

    @Transactional
    public void createFromPreOrder(Long sessionId, List<ReservationItem> preOrderItems) {
        List<ReservationItem> confirmed = preOrderItems.stream().filter(item -> item.getStatus() == PreOrderStatus.CONFIRMED).toList();
        if (confirmed.isEmpty() || orders.existsByServiceSessionIdAndSource(sessionId, OrderSource.PREORDER)) return;
        DiningOrder order = orders.save(new DiningOrder(sessionId, "Món khách chọn trước khi đặt bàn", OrderSource.PREORDER));
        confirmed.forEach(item -> items.save(new DiningOrderItem(order.getId(), item.getMenuItemId(), item.getItemNameSnapshot(),
                item.getUnitPrice(), item.getQuantity())));
    }

    public List<DiningOrderDtos.OrderResponse> kitchenBoard() {
        return orders.findAllByOrderByCreatedAtDesc().stream()
                .filter(order->List.of(DiningOrderStatus.SUBMITTED,DiningOrderStatus.PREPARING,DiningOrderStatus.READY).contains(order.getStatus()))
                .map(order->response(order,sessions.findById(order.getServiceSessionId()).orElseThrow())).toList();
    }

    @Transactional
    public DiningOrderDtos.OrderResponse kitchenStatus(Long id, DiningOrderStatus next) {
        DiningOrder order=find(id);DiningOrderStatus current=order.getStatus();
        boolean allowed=current==DiningOrderStatus.SUBMITTED&&next==DiningOrderStatus.PREPARING
                ||current==DiningOrderStatus.PREPARING&&next==DiningOrderStatus.READY;
        if(!allowed)throw new BusinessException("Chuyển trạng thái bếp không hợp lệ");
        List<DiningOrderItem> lines=items.findByOrderIdInOrderByIdAsc(List.of(id));
        try{
            if(next==DiningOrderStatus.PREPARING)lines.stream().filter(i->i.getStatus()==DiningOrderItemStatus.SUBMITTED).forEach(DiningOrderItem::preparing);
            if(next==DiningOrderStatus.READY)lines.stream().filter(i->List.of(DiningOrderItemStatus.PREPARING,DiningOrderItemStatus.DELAYED).contains(i.getStatus())).forEach(DiningOrderItem::ready);
        }catch(IllegalStateException ex){throw new BusinessException("Chuyển trạng thái bếp không hợp lệ");}
        syncOrder(order);return response(order,activeSession(order.getServiceSessionId()));
    }

    @Transactional
    public DiningOrderDtos.OrderResponse itemStatus(Long itemId,DiningOrderDtos.ItemStatusRequest request){
        DiningOrderItem item=items.findById(itemId).orElseThrow(()->new BusinessException("Không tìm thấy món"));
        DiningOrder order=find(item.getOrderId());
        try{
            switch(request.status()){
                case PREPARING->item.preparing();
                case DELAYED->{
                    if(request.delayMinutes()==null)throw new BusinessException("Hãy nhập số phút chậm");
                    item.delay(request.delayMinutes(),request.reason());
                    ServiceSession session=activeSession(order.getServiceSessionId());
                    Reservation reservation=reservations.findById(session.getReservationId()).orElseThrow();
                    notifications.createStaffAlert(reservation.getId(),NotificationType.KITCHEN_DELAY,
                            "Bếp báo chậm món",item.getItemNameSnapshot()+" chậm "+request.delayMinutes()+" phút"
                                    +(request.reason()==null?"":" – "+request.reason()),"kitchen-delay-"+item.getId()+"-"+item.getDelayedUntil());
                }
                case READY->item.ready();
                case SERVED->item.served();
                default->throw new BusinessException("Trạng thái món không hợp lệ");
            }
        }catch(IllegalStateException ex){throw new BusinessException("Chuyển trạng thái món không hợp lệ");}
        syncOrder(order);
        return response(order,activeSession(order.getServiceSessionId()));
    }

    @Transactional
    public DiningOrderDtos.OrderResponse serve(Long id) {
        DiningOrder order=find(id);if(order.getStatus()!=DiningOrderStatus.READY)throw new BusinessException("Món chưa sẵn sàng để phục vụ");
        items.findByOrderIdInOrderByIdAsc(List.of(id)).stream().filter(i->i.getStatus()==DiningOrderItemStatus.READY).forEach(DiningOrderItem::served);
        syncOrder(order);return response(order,activeSession(order.getServiceSessionId()));
    }

    @Transactional
    public DiningOrderDtos.OrderResponse cancel(Long id) {
        DiningOrder order=find(id);if(order.getStatus()!=DiningOrderStatus.SUBMITTED)throw new BusinessException("Chỉ có thể hủy phiếu bếp chưa tiếp nhận");
        order.changeStatus(DiningOrderStatus.CANCELLED);return response(order,activeSession(order.getServiceSessionId()));
    }

    private DiningOrder find(Long id){return orders.findById(id).orElseThrow(()->new BusinessException("Không tìm thấy phiếu gọi món"));}
    private ServiceSession activeSession(Long id){ServiceSession session=sessions.findById(id).orElseThrow(()->new BusinessException("Không tìm thấy phiên phục vụ"));if(session.getStatus()!=ServiceSessionStatus.ACTIVE)throw new BusinessException("Phiên phục vụ đã kết thúc");return session;}
    private DiningOrderDtos.OrderResponse response(DiningOrder order,ServiceSession session){
        Reservation reservation=reservations.findById(session.getReservationId()).orElseThrow();
        List<String> tableCodes=tables.findAllById(assignments.findByReservationId(reservation.getId()).stream().map(ReservationTableAssignment::getTableId).toList()).stream().map(t->t.getCode()).sorted().toList();
        List<DiningOrderDtos.ItemResponse> lines=items.findByOrderIdInOrderByIdAsc(List.of(order.getId())).stream().map(item->{BigDecimal lineTotal=item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));return new DiningOrderDtos.ItemResponse(item.getId(),item.getMenuItemId(),item.getItemNameSnapshot(),item.getUnitPrice(),item.getQuantity(),lineTotal,item.getStatus(),item.getPreparationMinutes(),item.getEstimatedReadyAt(),item.getStartedAt(),item.getDelayedUntil(),item.getDelayReason(),item.getReadyAt(),item.getServedAt());}).toList();
        BigDecimal total=lines.stream().map(DiningOrderDtos.ItemResponse::lineTotal).reduce(BigDecimal.ZERO,BigDecimal::add);
        return new DiningOrderDtos.OrderResponse(order.getId(),order.getServiceSessionId(),reservation.getId(),reservation.getCode(),reservation.getCustomerName(),tableCodes,order.getStatus(),order.getSource(),order.getNote(),order.getCreatedAt(),order.getUpdatedAt(),lines,total);
    }

    private void syncOrder(DiningOrder order){
        List<DiningOrderItem> lines=items.findByOrderIdInOrderByIdAsc(List.of(order.getId()));
        if(lines.stream().allMatch(i->i.getStatus()==DiningOrderItemStatus.SERVED))order.changeStatus(DiningOrderStatus.SERVED);
        else if(lines.stream().allMatch(i->List.of(DiningOrderItemStatus.READY,DiningOrderItemStatus.SERVED).contains(i.getStatus())))order.changeStatus(DiningOrderStatus.READY);
        else if(lines.stream().anyMatch(i->List.of(DiningOrderItemStatus.PREPARING,DiningOrderItemStatus.DELAYED,DiningOrderItemStatus.READY).contains(i.getStatus())))order.changeStatus(DiningOrderStatus.PREPARING);
        else order.changeStatus(DiningOrderStatus.SUBMITTED);
    }
}
