package com.roadbook.route;

import com.roadbook.amap.AmapClient;
import com.roadbook.amap.dto.GeoCodeResponse;
import com.roadbook.route.dto.RouteDetailResponse;
import com.roadbook.route.dto.RouteGenerateRequest;
import com.roadbook.route.service.RouteGenerateService;
import com.roadbook.template.entity.RouteTemplate;
import com.roadbook.template.entity.TemplateWaypoint;
import com.roadbook.template.repository.TemplateRepository;
import com.roadbook.template.repository.TemplateWaypointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class RouteGenerateServiceTest {

    @Autowired
    private RouteGenerateService routeGenerateService;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateWaypointRepository templateWaypointRepository;

    @MockBean
    private AmapClient amapClient;

    @BeforeEach
    void setUp() {
        // Seed a template
        RouteTemplate template = new RouteTemplate();
        template.setName("川西小环线");
        template.setRegion("四川省");
        template.setTotalDays(3);
        template.setTotalDistance(600);
        template.setDifficulty(2);
        template.setTags("[\"摄影\",\"雪山\"]");
        template.setUsageCount(10);
        template.setStatus(1);
        template = templateRepository.save(template);

        // Seed template waypoints for day 1
        TemplateWaypoint wp1 = new TemplateWaypoint();
        wp1.setTemplateId(template.getId());
        wp1.setDayNumber(1);
        wp1.setSortOrder(1);
        wp1.setPointType("scenic");
        wp1.setName("康定");
        wp1.setLng(BigDecimal.valueOf(101.9646));
        wp1.setLat(BigDecimal.valueOf(30.0540));
        wp1.setStayDuration(60);
        templateWaypointRepository.save(wp1);

        TemplateWaypoint wp2 = new TemplateWaypoint();
        wp2.setTemplateId(template.getId());
        wp2.setDayNumber(1);
        wp2.setSortOrder(2);
        wp2.setPointType("scenic");
        wp2.setName("折多山");
        wp2.setLng(BigDecimal.valueOf(101.7890));
        wp2.setLat(BigDecimal.valueOf(30.0785));
        wp2.setStayDuration(45);
        templateWaypointRepository.save(wp2);

        // Mock AmapClient.regeocode to return "四川省"
        GeoCodeResponse regeo = new GeoCodeResponse();
        GeoCodeResponse.RegeoCode regeoObj = new GeoCodeResponse.RegeoCode();
        GeoCodeResponse.AddressComponent ac = new GeoCodeResponse.AddressComponent();
        ac.setProvince("四川省");
        regeoObj.setAddressComponent(ac);
        regeo.setRegeocode(regeoObj);
        when(amapClient.regeocode(anyDouble(), anyDouble())).thenReturn(regeo);
    }

    @Test
    void shouldGenerateRouteFromTemplate() {
        RouteGenerateRequest req = buildRequest(3, "成都", 104.0657, 30.6574);
        RouteDetailResponse resp = routeGenerateService.generate(1L, req);

        assertNotNull(resp);
        assertNotNull(resp.getRouteId());
        assertNotNull(resp.getItinerary());
        assertFalse(resp.getItinerary().isEmpty());
        assertEquals(3, resp.getTotalDays());

        // Day 1 should have waypoints (康定, 折多山)
        RouteDetailResponse.DayItinerary day1 = resp.getItinerary().stream()
                .filter(d -> d.getDay() == 1)
                .findFirst().orElse(null);
        assertNotNull(day1);
        assertFalse(day1.getWaypoints().isEmpty());
        assertEquals("康定", day1.getWaypoints().get(0).getName());
    }

    @Test
    void shouldIncludeEstimatedCost() {
        RouteGenerateRequest req = buildRequest(3, "成都", 104.0657, 30.6574);
        RouteDetailResponse resp = routeGenerateService.generate(1L, req);

        assertNotNull(resp.getEstimatedCost());
        assertTrue(resp.getEstimatedCost().getTotalYuan() > 0);
    }

    private RouteGenerateRequest buildRequest(int days, String name, double lng, double lat) {
        RouteGenerateRequest req = new RouteGenerateRequest();
        req.setTotalDays(days);

        RouteGenerateRequest.PointInfo pt = new RouteGenerateRequest.PointInfo();
        pt.setName(name);
        pt.setLng(BigDecimal.valueOf(lng));
        pt.setLat(BigDecimal.valueOf(lat));
        req.setStartPoint(pt);
        req.setEndPoint(pt);

        RouteGenerateRequest.Preferences prefs = new RouteGenerateRequest.Preferences();
        prefs.setTags(List.of("摄影"));
        req.setPreferences(prefs);

        return req;
    }
}
