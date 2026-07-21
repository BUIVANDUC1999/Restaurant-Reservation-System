package com.khamphaviet.restaurant.table;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "restaurant_tables")
@Getter
@NoArgsConstructor
public class RestaurantTable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String floor;
    @Column(nullable = false) private String area;
    @Column(nullable = false) private Integer seats;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TableStatus status;
    @Column(nullable = false) private boolean active;

    public RestaurantTable(String code, String name, String floor, String area, int seats) {
        this.code = code; this.name = name; this.floor = floor; this.area = area; this.seats = seats;
        this.status = TableStatus.AVAILABLE; this.active = true;
    }
    public void changeStatus(TableStatus status) { this.status = status; this.active = status != TableStatus.INACTIVE; }
}

