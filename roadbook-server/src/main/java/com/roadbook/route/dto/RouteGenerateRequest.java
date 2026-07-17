package com.roadbook.route.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RouteGenerateRequest {

    @NotNull
    @Min(1)
    @Max(15)
    private Integer totalDays;

    @NotNull
    @Valid
    private PointInfo startPoint;

    @NotNull
    @Valid
    private PointInfo endPoint;

    @Valid
    private Preferences preferences;

    @Valid
    private VehicleInfo vehicleInfo;

    @Data
    public static class PointInfo {
        @NotNull
        private String name;

        @NotNull
        private BigDecimal lng;

        @NotNull
        private BigDecimal lat;
    }

    @Data
    public static class Preferences {
        private String difficulty = "easy";
        private List<String> tags;
        private Boolean preferScenicRoad = true;
        private Double dailyDriveHours = 4.0;
    }

    @Data
    public static class VehicleInfo {
        private String fuelType;
        private Integer rangeKm;
    }
}
