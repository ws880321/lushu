package com.roadbook.poi.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "pois")
public class Poi {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(length = 32)
    private String province;

    @Column(length = 32)
    private String city;

    @Column(length = 32)
    private String district;

    @Column(length = 256)
    private String address;

    @Column(length = 32)
    private String phone;

    @Column(length = 512)
    private String coverImage;

    @Column(columnDefinition = "JSON")
    private String images;

    @Column(precision = 2, scale = 1)
    private BigDecimal driveScore;

    @Column(precision = 2, scale = 1)
    private BigDecimal parkingScore;

    @Column(precision = 2, scale = 1)
    private BigDecimal roadScore;

    private Integer rvFriendly;

    @Column(nullable = false)
    private Integer campingAllowed = 0;

    private Integer signalQuality;

    @Column(nullable = false)
    private Integer petFriendly = 0;

    @Column(nullable = false, length = 16)
    private String source = "amap";

    @Column(length = 64)
    private String amapPoiId;

    @Column(nullable = false)
    private Integer confirmedCount = 0;

    @Column(nullable = false, updatable = false)
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
