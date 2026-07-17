package com.roadbook.template.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "route_templates")
public class RouteTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 64)
    private String region;

    @Column(nullable = false)
    private Integer totalDays;

    private Integer totalDistance;

    @Column(length = 64)
    private String bestSeason;

    @Column(nullable = false)
    private Integer difficulty = 1;

    @Column(columnDefinition = "JSON")
    private String tags;

    @Column(length = 512)
    private String coverImage;

    @Column(nullable = false)
    private Integer usageCount = 0;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
