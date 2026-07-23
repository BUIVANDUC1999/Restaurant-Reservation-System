package com.khamphaviet.restaurant.menu;

import com.khamphaviet.restaurant.common.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/staff/menu/items")
public class StaffMenuController {
    private final MenuItemRepository repository;

    public StaffMenuController(MenuItemRepository repository) { this.repository = repository; }

    public record MenuRequest(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 80) String category,
            @NotNull @DecimalMin("0") BigDecimal price,
            @Size(max = 600) String description,
            @Size(max = 600) String imageUrl,
            @NotNull Boolean featured,
            @NotNull Boolean available,
            @Min(5) @Max(240) Integer preparationMinutes) {}

    public record AvailabilityRequest(@NotNull Boolean available) {}

    @GetMapping
    public List<MenuItem> list() { return repository.findAllByOrderByNameAsc(); }

    @PostMapping
    @Transactional
    public MenuItem create(@Valid @RequestBody MenuRequest request) {
        return repository.save(new MenuItem(request.name(), request.category(), request.price(), request.description(),
                request.imageUrl(), request.featured(), request.available(),request.preparationMinutes()==null?20:request.preparationMinutes()));
    }

    @PutMapping("/{id}")
    @Transactional
    public MenuItem update(@PathVariable Long id, @Valid @RequestBody MenuRequest request) {
        MenuItem item = find(id);
        item.update(request.name(), request.category(), request.price(), request.description(), request.imageUrl(),
                request.featured(), request.available(),request.preparationMinutes()==null?item.getPreparationMinutes():request.preparationMinutes());
        return repository.save(item);
    }

    @PatchMapping("/{id}/availability")
    @Transactional
    public MenuItem availability(@PathVariable Long id, @Valid @RequestBody AvailabilityRequest request) {
        MenuItem item = find(id);
        item.setAvailable(request.available());
        return repository.save(item);
    }

    private MenuItem find(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException("Không tìm thấy món ăn"));
    }
}
