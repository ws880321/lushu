package com.roadbook.poi.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "poi_ratings")
public class PoiRating {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long poiId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal driveScore;

    @Column(precision = 2, scale = 1)
    private BigDecimal parkingScore;

    @Column(precision = 2, scale = 1)
    private BigDecimal roadScore;

    @Column(length = 512)
    private String comment;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
