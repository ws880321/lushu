package com.roadbook.alert.controller;

import com.roadbook.alert.dto.AlertResponse;
import com.roadbook.alert.service.AlertService;
import com.roadbook.common.ApiResponse;
import com.roadbook.common.ErrorCode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/nearby")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * Get nearby geofence alerts for the current user's location.
     * Alerts are categorized by severity (critical, warning, info) based on
     * POI type, distance, and vehicle range context.
     * <p>
     * Note: userId is extracted from the JWT interceptor for future
     * personalization (e.g., user-specific alert preferences).
     *
     * @param userId           authenticated user ID from JWT interceptor
     * @param lng              center longitude
     * @param lat              center latitude
     * @param radius           search radius in meters (default 30000 = 30km)
     * @param includeTypes     optional POI categories to filter
     * @param vehicleRangeLeft optional vehicle remaining range in km
     * @return categorized alerts with summary counts
     */
    @GetMapping("/alerts")
    public ApiResponse<AlertResponse> getAlerts(
            @RequestAttribute("userId") Long userId,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal lng,
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal lat,
            @RequestParam(defaultValue = "30000") @Min(1) int radius,
            @RequestParam(name = "include_types", required = false) List<String> includeTypes,
            @RequestParam(name = "vehicle_range_left", required = false) @Min(0) Integer vehicleRangeLeft) {

        AlertResponse response = alertService.getAlerts(lng, lat, radius, includeTypes, vehicleRangeLeft);
        return ApiResponse.success(response);
    }
}
