package com.roadbook.route.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class RouteDetailResponse {

    private Long routeId;
    private String title;
    private String description;
    private Integer totalDays;
    private Double totalDistanceKm;
    private EstimatedCost estimatedCost;
    private String weatherAlert;
    private List<DayItinerary> itinerary;
    private List<FuelStop> fuelStops;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class DayItinerary {
        private Integer day;
        private String date;
        private Double distanceKm;
        private Integer driveTimeMin;
        private String summary;
        private List<WaypointDetail> waypoints;
    }

    @Data
    @Builder
    public static class WaypointDetail {
        private Integer sort;
        private String type;
        private String name;
        private String description;
        private BigDecimal lng;
        private BigDecimal lat;
        private LocalTime arrival;
        private LocalTime departure;
        private Integer stayMin;
        private Double distanceFromPrevKm;
        private Integer driveScore;
        private Integer parkingScore;
        private Integer roadScore;
        private String tips;
        private Boolean isBreak;
        private String breakReason;
    }

    @Data
    @Builder
    public static class EstimatedCost {
        private Double tollYuan;
        private Double fuelYuan;
        private Double totalYuan;
    }

    @Data
    @Builder
    public static class FuelStop {
        private Integer day;
        private String location;
        private BigDecimal lng;
        private BigDecimal lat;
        private String reason;
    }
}
