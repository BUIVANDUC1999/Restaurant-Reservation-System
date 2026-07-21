package com.khamphaviet.restaurant.table;

import com.khamphaviet.restaurant.common.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/staff/tables")
public class TableController {
    private final RestaurantTableRepository repository;
    private final TableOverviewService overviewService;
    public TableController(RestaurantTableRepository repository, TableOverviewService overviewService) { this.repository = repository; this.overviewService = overviewService; }
    public record CreateRequest(@NotBlank String code, @NotBlank String name, @NotBlank String floor,
                                @NotBlank String area, @NotNull @Min(1) @Max(30) Integer seats) {}
    public record StatusRequest(@NotNull TableStatus status) {}

    @GetMapping public List<RestaurantTable> list() { return repository.findAllByOrderByFloorAscCodeAsc(); }
    @GetMapping("/overview") public List<TableOverviewService.TableOverview> overview() { return overviewService.list(); }
    @PostMapping public RestaurantTable create(@Valid @RequestBody CreateRequest request) {
        if (repository.existsByCodeIgnoreCase(request.code())) throw new BusinessException("Mã bàn đã tồn tại");
        return repository.save(new RestaurantTable(request.code().trim().toUpperCase(), request.name().trim(),
                request.floor().trim(), request.area().trim(), request.seats()));
    }
    @PatchMapping("/{id}/status") public RestaurantTable status(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        RestaurantTable table = repository.findById(id).orElseThrow(() -> new BusinessException("Không tìm thấy bàn"));
        table.changeStatus(request.status()); return repository.save(table);
    }
}
