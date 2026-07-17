package com.roadbook.alert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {

    private List<AlertItem> alerts;
    private Map<String, Integer> summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertItem {
        private String type;          // fuel_warning, geo_surprise, parking_tip, weather_warning
        private String level;         // critical, warning, info
        private String title;
        private String description;
        private String poiName;
        private BigDecimal poiLng;
        private BigDecimal poiLat;
        private Integer distanceM;
    }
}
