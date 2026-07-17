package com.roadbook.template.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "template_waypoints")
public class TemplateWaypoint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long templateId;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private Integer dayNumber;

    @Column(nullable = false, length = 32)
    private String pointType;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal lat;

    private Long poiId;

    private Integer stayDuration;

    @Column(length = 512)
    private String tips;
}
