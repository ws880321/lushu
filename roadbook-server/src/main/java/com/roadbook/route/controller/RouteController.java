package com.roadbook.route.controller;

import com.roadbook.common.ApiResponse;
import com.roadbook.common.ErrorCode;
import com.roadbook.route.dto.RouteDetailResponse;
import com.roadbook.route.dto.RouteGenerateRequest;
import com.roadbook.route.entity.Route;
import com.roadbook.route.entity.RouteWaypoint;
import com.roadbook.route.dto.RouteDetailResponse.DayItinerary;
import com.roadbook.route.dto.RouteDetailResponse.EstimatedCost;
import com.roadbook.route.dto.RouteDetailResponse.FuelStop;
import com.roadbook.route.dto.RouteDetailResponse.WaypointDetail;
import com.roadbook.route.service.RouteGenerateService;
import com.roadbook.route.service.RouteService;
import com.roadbook.template.entity.RouteTemplate;
import com.roadbook.template.service.TemplateService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final RouteGenerateService routeGenerateService;
    private final RouteService routeService;
    private final TemplateService templateService;

    public RouteController(RouteGenerateService routeGenerateService, RouteService routeService,
                           TemplateService templateService) {
        this.routeGenerateService = routeGenerateService;
        this.routeService = routeService;
        this.templateService = templateService;
    }

    /**
     * Generate a new route (roadbook) based on user preferences and matched template.
     *
     * @param userId authenticated user ID from JWT interceptor
     * @param req    generation request with trip parameters
     * @return detailed route response
     */
    @PostMapping("/generate")
    public ApiResponse<?> generate(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody RouteGenerateRequest req) {
        try {
            RouteDetailResponse response = routeGenerateService.generate(userId, req);
            return ApiResponse.success(response);
        } catch (RuntimeException e) {
            if ("TEMPLATE_NOT_FOUND".equals(e.getMessage())) {
                ApiResponse<Map<String, Object>> resp = ApiResponse.error(ErrorCode.TEMPLATE_NOT_FOUND, "未找到匹配模板");
                String region = "四川";
                List<RouteTemplate> alts = templateService.getAlternatives(region, req.getTotalDays());
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("suggestion", "为您推荐以下路线方案");
                data.put("alternatives", alts.stream().map(t -> Map.of(
                    "id", t.getId(), "name", t.getName(), "region", t.getRegion(),
                    "totalDays", t.getTotalDays(), "difficulty", t.getDifficulty()
                )).toList());
                resp.setData(data);
                return resp;
            }
            log.error("Route generation failed for user {}: {}", userId, e.getMessage(), e);
            return ApiResponse.error(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * Get route detail with waypoints.
     *
     * @param id route ID
     * @return route detail with waypoints
     */
    @GetMapping("/{id}")
    public ApiResponse<RouteDetailResponse> getDetail(@PathVariable Long id) {
        Route route = routeService.getById(id);
        if (route == null) {
            return ApiResponse.error(ErrorCode.NOT_FOUND);
        }
        List<RouteWaypoint> routeWaypoints = routeService.getWaypoints(id);
        RouteDetailResponse response = buildDetailResponse(route, routeWaypoints);
        return ApiResponse.success(response);
    }

    /**
     * List routes for the current user, paginated.
     *
     * @param userId authenticated user ID from JWT interceptor
     * @param page   page number (0-based, default 0)
     * @param size   page size (default 10)
     * @return paginated route list
     */
    @GetMapping
    public ApiResponse<Page<Route>> list(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Route> routes = routeService.listByUser(userId, page, size);
        return ApiResponse.success(routes);
    }

    // ========== Private helpers ==========

    /**
     * Build a RouteDetailResponse from stored Route and RouteWaypoint entities.
     */
    private RouteDetailResponse buildDetailResponse(Route route, List<RouteWaypoint> waypoints) {
        if (waypoints == null) {
            waypoints = Collections.emptyList();
        }

        // Group waypoints by day
        Map<Integer, List<RouteWaypoint>> dayGroups = waypoints.stream()
                .collect(Collectors.groupingBy(RouteWaypoint::getDayNumber, TreeMap::new, Collectors.toList()));

        List<DayItinerary> itinerary = new ArrayList<>();
        List<FuelStop> fuelStops = new ArrayList<>();
        double totalDistanceKm = 0.0;

        for (Map.Entry<Integer, List<RouteWaypoint>> entry : dayGroups.entrySet()) {
            int day = entry.getKey();
            List<RouteWaypoint> dayWaypoints = entry.getValue();

            double dayDistanceKm = 0;
            int driveTimeMin = 0;
            List<WaypointDetail> details = new ArrayList<>();

            for (RouteWaypoint rwp : dayWaypoints) {
                double distKm = rwp.getDistanceFromPrev() != null
                        ? rwp.getDistanceFromPrev() / 1000.0
                        : 0.0;
                dayDistanceKm += distKm;
                int driveMin = distKm > 0 ? (int) Math.ceil(distKm / 60.0 * 60.0) : 0;
                driveTimeMin += driveMin;

                boolean isBreak = rwp.getIsBreakPoint() != null && rwp.getIsBreakPoint() == 1;

                WaypointDetail detail = WaypointDetail.builder()
                        .sort(rwp.getSortOrder())
                        .type(rwp.getPointType())
                        .name(rwp.getName())
                        .description(rwp.getDescription())
                        .lng(rwp.getLng())
                        .lat(rwp.getLat())
                        .arrival(rwp.getArrivalTime())
                        .departure(rwp.getDepartureTime())
                        .stayMin(rwp.getStayDuration())
                        .distanceFromPrevKm(distKm > 0 ? Math.round(distKm * 10.0) / 10.0 : 0.0)
                        .isBreak(isBreak)
                        .build();
                details.add(detail);

                String type = rwp.getPointType() != null ? rwp.getPointType() : "";
                if ("gas".equals(type) || "charging".equals(type)) {
                    fuelStops.add(FuelStop.builder()
                            .day(day)
                            .location(rwp.getName())
                            .lng(rwp.getLng())
                            .lat(rwp.getLat())
                            .reason("gas".equals(type) ? "加油" : "充电")
                            .build());
                }
            }

            totalDistanceKm += dayDistanceKm;

            DayItinerary dayItem = DayItinerary.builder()
                    .day(day)
                    .distanceKm(Math.round(dayDistanceKm * 10.0) / 10.0)
                    .driveTimeMin(driveTimeMin)
                    .waypoints(details)
                    .build();
            itinerary.add(dayItem);
        }

        EstimatedCost cost = EstimatedCost.builder()
                .tollYuan(Math.round(totalDistanceKm * 0.15 * 100.0) / 100.0)
                .fuelYuan(Math.round(totalDistanceKm * 0.5 * 100.0) / 100.0)
                .totalYuan(Math.round(totalDistanceKm * 0.65 * 100.0) / 100.0)
                .build();

        return RouteDetailResponse.builder()
                .routeId(route.getId())
                .title(route.getTitle())
                .description(route.getDescription())
                .totalDays(route.getTotalDays())
                .totalDistanceKm(Math.round(totalDistanceKm * 10.0) / 10.0)
                .estimatedCost(cost)
                .itinerary(itinerary)
                .fuelStops(fuelStops)
                .createdAt(route.getCreatedAt())
                .build();
    }
}
