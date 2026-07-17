package com.roadbook.alert;

import com.roadbook.alert.dto.AlertResponse;
import com.roadbook.alert.service.AlertService;
import com.roadbook.poi.repository.PoiRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class AlertServiceTest {

    @Autowired
    private AlertService alertService;

    @MockBean
    private PoiRepository poiRepository;

    @Test
    void shouldReturnEmptyAlertsForNoPOIs() {
        when(poiRepository.findNearby(any(BigDecimal.class), any(BigDecimal.class),
                anyInt(), anyList())).thenReturn(Collections.emptyList());

        AlertResponse resp = alertService.getAlerts(
                BigDecimal.valueOf(104.0657), BigDecimal.valueOf(30.6574),
                30000, List.of("gas"), null);

        assertNotNull(resp);
        assertTrue(resp.getAlerts().isEmpty());
        assertNotNull(resp.getSummary());
        assertEquals(0, resp.getSummary().get("critical").intValue());
        assertEquals(0, resp.getSummary().get("warning").intValue());
        assertEquals(0, resp.getSummary().get("info").intValue());
    }

    @Test
    void shouldReturnFuelAlertWhenVehicleRangeLow() {
        List<Object[]> mockResults = new ArrayList<>();
        mockResults.add(new Object[]{
                1L,                          // id
                "中石化加油站",               // name
                "gas",                       // category
                BigDecimal.valueOf(104.07),   // lng
                BigDecimal.valueOf(30.66),    // lat
                "四川省",                      // province
                "成都市",                      // city
                "武侯区",                      // district
                "人民南路四段",               // address
                "028-12345678",               // phone
                null,                         // cover_image
                null,                         // images
                BigDecimal.valueOf(4.5),      // drive_score
                BigDecimal.valueOf(4.0),      // parking_score
                BigDecimal.valueOf(4.0),      // road_score
                1,                            // rv_friendly
                0,                            // camping_allowed
                5,                            // signal_quality
                1,                            // pet_friendly
                "amap",                       // source
                null,                         // amap_poi_id
                10,                           // confirmed_count
                null,                         // created_at
                null,                         // updated_at
                BigDecimal.valueOf(5000)      // distance_m
        });
        when(poiRepository.findNearby(any(BigDecimal.class), any(BigDecimal.class),
                anyInt(), anyList())).thenReturn(mockResults);

        AlertResponse resp = alertService.getAlerts(
                BigDecimal.valueOf(104.0657), BigDecimal.valueOf(30.6574),
                30000, List.of("gas"), 60);

        assertNotNull(resp);
        assertFalse(resp.getAlerts().isEmpty());

        // With range 60km and POI 5km away, should be info (not critical since 60 > 5 + 50 buffer)
        AlertResponse.AlertItem first = resp.getAlerts().get(0);
        assertEquals("info", first.getLevel());
    }

    @Test
    void shouldReturnCriticalAlertWhenRangeVeryLow() {
        List<Object[]> mockResults = new ArrayList<>();
        mockResults.add(new Object[]{
                1L,
                "中石化加油站",
                "gas",
                BigDecimal.valueOf(104.07),
                BigDecimal.valueOf(30.66),
                "四川省",
                "成都市",
                "武侯区",
                "人民南路四段",
                "028-12345678",
                null, null,
                BigDecimal.valueOf(4.5),
                BigDecimal.valueOf(4.0),
                BigDecimal.valueOf(4.0),
                1, 0, 5, 1,
                "amap", null, 10, null, null,
                BigDecimal.valueOf(60000)  // 60km away
        });
        when(poiRepository.findNearby(any(BigDecimal.class), any(BigDecimal.class),
                anyInt(), anyList())).thenReturn(mockResults);

        // Range is 60, POI is 60km away, buffer is 50km: 60 < 60+50 => critical
        AlertResponse resp = alertService.getAlerts(
                BigDecimal.valueOf(104.0657), BigDecimal.valueOf(30.6574),
                30000, List.of("gas"), 60);

        assertNotNull(resp);
        assertFalse(resp.getAlerts().isEmpty());
        assertEquals("critical", resp.getAlerts().get(0).getLevel());
        assertEquals("fuel_warning", resp.getAlerts().get(0).getType());
    }

    @Test
    void shouldHandleNullTypesGracefully() {
        when(poiRepository.findNearby(any(BigDecimal.class), any(BigDecimal.class),
                anyInt(), anyList())).thenReturn(Collections.emptyList());

        AlertResponse resp = alertService.getAlerts(
                BigDecimal.valueOf(104.0657), BigDecimal.valueOf(30.6574),
                30000, null, null);

        assertNotNull(resp);
        assertTrue(resp.getAlerts().isEmpty());
    }
}
