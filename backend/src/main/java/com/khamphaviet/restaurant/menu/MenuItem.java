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
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String category;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    private String description;
    private String imageUrl;
    @Column(nullable = false) private boolean featured;
    @Column(nullable = false) private boolean available;
}

