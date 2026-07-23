package com.khamphaviet.restaurant.table;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;
import java.time.Instant;

@Entity
@Table(name = "restaurant_tables")
@Getter
@NoArgsConstructor
public class RestaurantTable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String floor;
    @Column(nullable = false) private String area;
    @Column(nullable = false) private Integer seats;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TableStatus status;
    @Column(nullable = false) private boolean active;
    @Column(nullable = false) private Integer layoutX;
    @Column(nullable = false) private Integer layoutY;
    @Column(nullable = false, length = 20) private String shape;
    @Column(nullable = false, unique = true, length = 64) private String publicToken;
    @Column(nullable = false) private Instant statusChangedAt;

    public RestaurantTable(String code, String name, String floor, String area, int seats) {
        this(code, name, floor, area, seats, 0, 0, "ROUND");
    }
    public RestaurantTable(String code, String name, String floor, String area, int seats, int layoutX, int layoutY, String shape) {
        this.code = code; this.name = name; this.floor = floor; this.area = area; this.seats = seats;
        this.status = TableStatus.AVAILABLE; this.active = true;
        this.layoutX = layoutX; this.layoutY = layoutY; this.shape = shape; this.publicToken = UUID.randomUUID().toString();
        this.statusChangedAt = Instant.now();
    }
    public void changeStatus(TableStatus status) {
        if (this.status != status) this.statusChangedAt = Instant.now();
        this.status = status; this.active = status != TableStatus.INACTIVE;
    }
    public void updateLayout(int x, int y, String shape) { this.layoutX=x; this.layoutY=y; this.shape=shape; }
}

