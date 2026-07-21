package com.khamphaviet.restaurant.order;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/staff")
public class StaffOrderController {
    private final DiningOrderService service;
    public StaffOrderController(DiningOrderService service){this.service=service;}
    @PostMapping("/service-sessions/{sessionId}/orders")
    public DiningOrderDtos.OrderResponse create(@PathVariable Long sessionId,@Valid @RequestBody DiningOrderDtos.CreateRequest request){return service.create(sessionId,request);}
    @GetMapping("/service-sessions/{sessionId}/orders")
    public List<DiningOrderDtos.OrderResponse> list(@PathVariable Long sessionId){return service.listForSession(sessionId);}
    @PatchMapping("/orders/{id}/served")
    public DiningOrderDtos.OrderResponse serve(@PathVariable Long id){return service.serve(id);}
    @PatchMapping("/orders/{id}/cancel")
    public DiningOrderDtos.OrderResponse cancel(@PathVariable Long id){return service.cancel(id);}
}
