package com.roadbook.alert.service;

import com.roadbook.alert.dto.AlertResponse;
import com.roadbook.alert.dto.AlertResponse.AlertItem;
import com.roadbook.common.ApiResponse;
import com.roadbook.common.ErrorCode;
import com.roadbook.poi.repository.PoiRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AlertService {

    private static final int RANGE_BUFFER_KM = 50;

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

        // Query nearby POIs
        List<Object[]> rawResults = poiRepository.findNearby(lng, lat, radius, categoryList);

        List<AlertItem> alerts = new ArrayList<>();
        int criticalCount = 0;
        int warningCount = 0;
        int infoCount = 0;

        // Column index contract (must match PoiRepository.findNearby query):
        // [0]=id, [1]=name, [2]=category, [3]=lng, [4]=lat, [5]=province, [6]=city,
        // [7]=district, [8]=address, [9]=phone, [10]=cover_image, [11]=images,
        // [12]=drive_score, [13]=parking_score, [14]=road_score, [15]=rv_friendly,
        // [16]=camping_allowed, [17]=signal_quality, [18]=pet_friendly, [19]=source,
        // [20]=amap_poi_id, [21]=confirmed_count, [22]=created_at, [23]=updated_at,
        // [24]=distance_m
        for (Object[] row : rawResults) {
            String poiName = (String) row[1];
            String category = (String) row[2];
            BigDecimal poiLng = (BigDecimal) row[3];
            BigDecimal poiLat = (BigDecimal) row[4];
            BigDecimal distanceM = (BigDecimal) row[24];

            int distanceKm = distanceM.divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_UP).intValue();
            BigDecimal parkingScore = row[13] != null ? (BigDecimal) row[13] : BigDecimal.ZERO;

            // Classify alert
            String level;
            String type;
            String title;
            String description;

            if ("gas".equals(category) || "charging".equals(category)) {
                // Fuel/charging alert — critical if approaching range limit
                if (vehicleRangeLeft != null && vehicleRangeLeft < distanceKm + RANGE_BUFFER_KM) {
                    level = "critical";
                    type = "fuel_warning";
                    title = "电量/油量告急";
                    description = String.format(
                            "前方%d公里处有%s，当前续航约%d公里，建议及时补充",
                            distanceKm, "gas".equals(category) ? "加油站" : "充电站", vehicleRangeLeft);
                } else {
                    level = "info";
                    type = "fuel_warning";
                    title = "沿途补给点";
                    description = String.format("前方%d公里处有%s", distanceKm, "gas".equals(category) ? "加油站" : "充电站");
                }
            } else if ("parking".equals(category)) {
                level = "warning";
                type = "parking_tip";
                title = "停车提醒";
                if (parkingScore.compareTo(BigDecimal.valueOf(3)) < 0) {
                    description = String.format(
                            "前方%d公里处有停车场，但停车评分较低(%.1f)，建议提前规划", distanceKm, parkingScore);
                } else {
                    description = String.format("前方%d公里处有停车场，评分%.1f", distanceKm, parkingScore);
                }
            } else {
                // scenic, restaurant, other — info level surprise recommendation
                level = "info";
                type = "geo_surprise";
                title = "沿途惊喜";
                String categoryLabel = getCategoryLabel(category);
                description = String.format("前方%d公里处有%s「%s」", distanceKm, categoryLabel, poiName);
            }

            AlertItem item = AlertItem.builder()
                    .type(type)
                    .level(level)
                    .title(title)
                    .description(description)
                    .poiName(poiName)
                    .poiLng(poiLng)
                    .poiLat(poiLat)
                    .distanceM(distanceM.intValue())
                    .build();

            alerts.add(item);

            switch (level) {
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
