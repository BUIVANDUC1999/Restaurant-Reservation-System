package com.khamphaviet.restaurant.menu;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/menu/items")
public class MenuController {
    private final MenuItemRepository repository;
    public MenuController(MenuItemRepository repository) { this.repository = repository; }

    @GetMapping
    public List<MenuItem> list(@RequestParam(required = false) String category) {
        return repository.findByAvailableTrueOrderByFeaturedDescNameAsc().stream()
                .filter(item -> category == null || category.isBlank() || item.getCategory().equalsIgnoreCase(category))
                .toList();
    }
}

