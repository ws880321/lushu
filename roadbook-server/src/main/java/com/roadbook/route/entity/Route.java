package com.roadbook.route.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "routes")
public class Route {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 1024)
    private String description;

    @Column(nullable = false)
    private Integer totalDays;

    @Column(nullable = false, length = 128)
    private String startPoint;

    @Column(nullable = false, length = 128)
    private String endPoint;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal startLng;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal startLat;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal endLng;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal endLat;

    private Integer totalDistance;

    @Column(columnDefinition = "JSON")
    private String tags;

    @Column(length = 512)
    private String thumbnailUrl;

    @Column(nullable = false)
    private Integer status = 1;

    private Long templateId;

    @Column(nullable = false)
    private Integer isPublic = 0;

    @Column(nullable = false)
    private Integer viewCount = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
