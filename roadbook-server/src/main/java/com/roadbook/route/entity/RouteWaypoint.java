package com.roadbook.route.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "route_waypoints")
public class RouteWaypoint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long routeId;

    private Long poiId;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private Integer dayNumber;

    @Column(nullable = false, length = 32)
    private String pointType = "scenic";

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal lat;

    private LocalTime arrivalTime;

    private LocalTime departureTime;

    private Integer stayDuration;

    private Integer distanceFromPrev;

    @Column(nullable = false)
    private Integer isBreakPoint = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
