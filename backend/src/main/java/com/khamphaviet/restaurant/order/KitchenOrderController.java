package com.khamphaviet.restaurant.order;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/kitchen/orders")
public class KitchenOrderController {
    private final DiningOrderService service;
    public KitchenOrderController(DiningOrderService service){this.service=service;}
    @GetMapping public List<DiningOrderDtos.OrderResponse> list(){return service.kitchenBoard();}
    @PatchMapping("/{id}/status")
    public DiningOrderDtos.OrderResponse update(@PathVariable Long id,@Valid @RequestBody DiningOrderDtos.StatusRequest request){return service.kitchenStatus(id,request.status());}
    @PatchMapping("/items/{id}/status")
    public DiningOrderDtos.OrderResponse updateItem(@PathVariable Long id,@Valid @RequestBody DiningOrderDtos.ItemStatusRequest request){return service.itemStatus(id,request);}
}
