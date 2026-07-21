package com.khamphaviet.restaurant.menu;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "menu_items")
@Getter
@NoArgsConstructor
public class MenuItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 160) private String name;
    @Column(nullable = false, length = 80) private String category;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Column(length = 600) private String description;
    @Column(length = 600) private String imageUrl;
    @Column(nullable = false) private boolean featured;
    @Column(nullable = false) private boolean available;

    public MenuItem(String name, String category, BigDecimal price, String description, String imageUrl, boolean featured, boolean available) {
        update(name, category, price, description, imageUrl, featured, available);
    }

    public void update(String name, String category, BigDecimal price, String description, String imageUrl, boolean featured, boolean available) {
        this.name = name.trim();
        this.category = category.trim();
        this.price = price;
        this.description = description == null ? null : description.trim();
        this.imageUrl = imageUrl == null ? null : imageUrl.trim();
        this.featured = featured;
        this.available = available;
    }

    public void setAvailable(boolean available) { this.available = available; }
}
