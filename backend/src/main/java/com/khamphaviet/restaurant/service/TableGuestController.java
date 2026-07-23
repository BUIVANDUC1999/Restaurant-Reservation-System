package com.khamphaviet.restaurant.service;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1")
public class TableGuestController {
    private final TableGuestService service;
    public TableGuestController(TableGuestService service){this.service=service;}
    @GetMapping("/table-guest/{token}") public TableGuestService.GuestTable view(@PathVariable String token){return service.view(token);}
    @PostMapping("/table-guest/{token}/requests") public TableServiceRequest create(@PathVariable String token,@RequestBody TableGuestService.CreateRequest request){return service.create(token,request);}
    @GetMapping("/staff/table-requests") public List<TableServiceRequest> requests(){return service.staffFeed();}
    public record StatusBody(TableRequestStatus status){}
    @PatchMapping("/staff/table-requests/{id}") public TableServiceRequest update(@PathVariable Long id,@RequestBody StatusBody body){return service.update(id,body.status());}
}
