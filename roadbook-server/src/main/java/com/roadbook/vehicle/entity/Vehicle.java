package com.roadbook.vehicle.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "vehicles")
public class Vehicle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 64)
    private String name;

    @Column(length = 64)
    private String brand;

    @NotNull @NotBlank
    @Column(nullable = false, length = 16)
    private String fuelType;

    @Column(precision = 8, scale = 2)
    private BigDecimal tankCapacity;

    @Column(precision = 8, scale = 2)
    private BigDecimal avgConsumption;

    private Integer rangeFull;

    @Column(length = 16)
    private String plateNumber;

    @Column(nullable = false)
    private Integer isDefault = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
