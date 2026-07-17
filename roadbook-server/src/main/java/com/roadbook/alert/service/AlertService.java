package com.roadbook.alert.service;

import com.roadbook.alert.dto.AlertResponse;
import com.roadbook.alert.dto.AlertResponse.AlertItem;
import com.roadbook.common.ApiResponse;
import com.roadbook.common.ErrorCode;
import com.roadbook.poi.entity.Poi;
import com.roadbook.poi.repository.PoiRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
public class AlertService {

    private static final int RANGE_BUFFER_KM = 50;
    private static final double METERS_PER_LNG = 85390.0;
    private static final double METERS_PER_LAT = 111320.0;

    private final PoiRepository poiRepository;

    public AlertService(PoiRepository poiRepository) {
        this.poiRepository = poiRepository;
    }

    /**
     * Get nearby alerts for a given location and vehicle context.
     *
     * @param lng              center longitude
     * @param lat              center latitude
     * @param radius           search radius in meters
     * @param includeTypes     optional comma-separated category filter (e.g. "gas,charging")
     * @param vehicleRangeLeft vehicle remaining range in km (null if unknown)
     * @return AlertResponse with categorized alerts and summary
     */
    public AlertResponse getAlerts(BigDecimal lng, BigDecimal lat, int radius,
                                   List<String> includeTypes, Integer vehicleRangeLeft) {
        // Normalize to null to avoid empty-list errors in native IN clause
        List<String> categoryList = (includeTypes != null && !includeTypes.isEmpty()) ? includeTypes : null;

        // Query nearby POIs — try native MySQL first, fallback to H2-compatible
        List<AlertItem> alerts = new ArrayList<>();
        int criticalCount = 0;
        int warningCount = 0;
        int infoCount = 0;
        double radiusSq = (double) radius * (double) radius;

        try {
            List<Object[]> rawResults = poiRepository.findNearby(lng, lat, radius, categoryList);
            for (Object[] row : rawResults) {
                String poiName = (String) row[1];
                String category = (String) row[2];
                BigDecimal poiLng = (BigDecimal) row[3];
                BigDecimal poiLat = (BigDecimal) row[4];
                BigDecimal distanceM = (BigDecimal) row[24];
                int distanceKm = distanceM.divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_UP).intValue();
                BigDecimal parkingScore = row[13] != null ? (BigDecimal) row[13] : BigDecimal.ZERO;
                addAlert(alerts, poiName, category, poiLng, poiLat, distanceKm, (int)distanceM.doubleValue(), parkingScore, vehicleRangeLeft);
            }
        } catch (Exception e) {
            // H2 fallback: use simple coordinate distance
            log.debug("Native spatial query failed, using H2 fallback: {}", e.getMessage());
            List<Poi> pois = poiRepository.findNearbySimple(lng, lat, radiusSq, categoryList);
            for (Poi p : pois) {
                double dx = p.getLng().subtract(lng).doubleValue() * METERS_PER_LNG;
                double dy = p.getLat().subtract(lat).doubleValue() * METERS_PER_LAT;
                int distM = (int) Math.sqrt(dx * dx + dy * dy);
                int distKm = distM / 1000;
                BigDecimal parkingScore = p.getParkingScore() != null ? p.getParkingScore() : BigDecimal.ZERO;
                addAlert(alerts, p.getName(), p.getCategory(), p.getLng(), p.getLat(), distKm, distM, parkingScore, vehicleRangeLeft);
            }
        }

        // Count alert levels
        for (AlertItem a : alerts) {
            switch (a.getLevel()) {
                case "critical" -> criticalCount++;
                case "warning" -> warningCount++;
                default -> infoCount++;
            }
        }

        // Sort: critical first, then warning, then info
        alerts.sort(Comparator.comparingInt(a -> switch (a.getLevel()) {
            case "critical" -> 0;
            case "warning" -> 1;
            default -> 2;
        }));

        Map<String, Integer> summary = new LinkedHashMap<>();
        summary.put("critical", criticalCount);
        summary.put("warning", warningCount);
        summary.put("info", infoCount);

        return AlertResponse.builder()
                .alerts(alerts)
                .summary(summary)
                .build();
    }

    private void addAlert(List<AlertItem> alerts, String poiName, String category,
                          BigDecimal poiLng, BigDecimal poiLat, int distKm, int distM,
                          BigDecimal parkingScore, Integer vehicleRangeLeft) {
        String level;
        String type;
        String title;
        String description;

        if ("gas".equals(category) || "charging".equals(category)) {
            if (vehicleRangeLeft != null && vehicleRangeLeft < distKm + RANGE_BUFFER_KM) {
                level = "critical"; type = "fuel_warning"; title = "油量/电量告急";
                description = String.format("前方%d公里处有%s，续航约%d公里建议及时补给", distKm,
                    "gas".equals(category) ? "加油站" : "充电站", vehicleRangeLeft);
            } else {
                level = "info"; type = "fuel_warning"; title = "沿途补给点";
                description = String.format("前方%d公里处有%s", distKm, "gas".equals(category) ? "加油站" : "充电站");
            }
        } else if ("parking".equals(category)) {
            level = "warning"; type = "parking_tip"; title = "停车提醒";
            if (parkingScore.compareTo(BigDecimal.valueOf(3)) < 0) {
                description = String.format("前方%d公里处停车场评分%.1f较低，建议提前规划", distKm, parkingScore);
            } else {
                description = String.format("前方%d公里处有停车场评分%.1f", distKm, parkingScore);
            }
        } else {
            level = "info"; type = "geo_surprise"; title = "沿途惊喜";
            description = String.format("前方%d公里处有%s「%s」", distKm, getCategoryLabel(category), poiName);
        }

        alerts.add(AlertItem.builder().type(type).level(level).title(title).description(description)
                .poiName(poiName).poiLng(poiLng).poiLat(poiLat).distanceM(distM).build());
    }

    /**
     * Map POI category codes to human-readable Chinese labels.
     */
    private String getCategoryLabel(String category) {
        return switch (category != null ? category : "") {
            case "scenic" -> "景点";
            case "restaurant" -> "餐厅";
            case "gas" -> "加油站";
            case "charging" -> "充电站";
            case "parking" -> "停车场";
            case "hotel" -> "酒店";
            case "supermarket" -> "超市";
            case "hospital" -> "医院";
            default -> "服务设施";
        };
    }
}
