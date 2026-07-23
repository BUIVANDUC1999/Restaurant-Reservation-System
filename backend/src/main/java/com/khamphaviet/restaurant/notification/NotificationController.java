package com.khamphaviet.restaurant.notification;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/staff/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service){this.service=service;}
    @GetMapping public List<OperationalNotification> list(){return service.staffFeed();}
    @PatchMapping("/{id}/read") public void read(@PathVariable Long id){service.markRead(id);}
}
