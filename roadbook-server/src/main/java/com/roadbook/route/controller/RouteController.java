package com.roadbook.route.controller;

import com.roadbook.common.ApiResponse;
import com.roadbook.common.ErrorCode;
import com.roadbook.route.dto.*;
import com.roadbook.route.dto.RouteDetailResponse.DayItinerary;
import com.roadbook.route.dto.RouteDetailResponse.EstimatedCost;
import com.roadbook.route.dto.RouteDetailResponse.FuelStop;
import com.roadbook.route.dto.RouteDetailResponse.WaypointDetail;
import com.roadbook.route.entity.Route;
import com.roadbook.route.entity.RouteWaypoint;
import com.roadbook.route.repository.RouteRepository;
import com.roadbook.route.repository.RouteWaypointRepository;
import com.roadbook.route.service.RouteGenerateService;
import com.roadbook.route.service.RouteService;
import com.roadbook.ai.AiRouteGenerator;
import com.roadbook.template.entity.RouteTemplate;
import com.roadbook.template.service.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final RouteGenerateService routeGenerateService;
    private final RouteService routeService;
    private final TemplateService templateService;
    private final RouteWaypointRepository waypointRepo;
    private final RouteRepository routeRepo;
    private final com.roadbook.poi.repository.PoiRepository poiRepo;
    private final AiRouteGenerator aiGenerator;
    private final ObjectMapper objectMapper;

    public RouteController(RouteGenerateService routeGenerateService, RouteService routeService,
                           TemplateService templateService, RouteWaypointRepository waypointRepo,
                           RouteRepository routeRepo, com.roadbook.poi.repository.PoiRepository poiRepo,
                           AiRouteGenerator aiGenerator, ObjectMapper objectMapper) {
        this.routeGenerateService = routeGenerateService;
        this.routeService = routeService;
        this.templateService = templateService;
        this.waypointRepo = waypointRepo;
        this.routeRepo = routeRepo;
        this.poiRepo = poiRepo;
        this.aiGenerator = aiGenerator;
        this.objectMapper = objectMapper;
    }

    /**
     * Record a manual trip waypoint — user documents their own journey.
     */
    @PostMapping("/record")
    public ApiResponse<Map<String, Object>> recordWaypoint(
            @RequestAttribute("userId") Long userId,
            @RequestBody RecordWaypointRequest req) {
        // Find or create active recording route
        Route route = routeRepo.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, 2).orElse(null);
        if (route == null) {
            // Re-recording on a specific existing route (status=1 or status=2)
            Route existing = routeRepo.findById(req.getRouteId() != null ? req.getRouteId() : -1L).orElse(null);
            if (existing != null) {
                route = existing;
                // Set back to recording if it was finished
                if (route.getStatus() != 2) {
                    route.setStatus(2);
                    routeRepo.save(route);
                }
            }
        }
        if (route == null) {
            route = new Route();
            route.setUserId(userId);
            route.setTitle("行程记录 " + LocalDateTime.now().toString().substring(0, 10));
            route.setTotalDays(1);
            route.setStartPoint(req.getName() != null ? req.getName() : "新行程");
            route.setEndPoint(req.getName() != null ? req.getName() : "待定");
            route.setStartLng(req.getLng() != null ? BigDecimal.valueOf(req.getLng()) : BigDecimal.ZERO);
            route.setStartLat(req.getLat() != null ? BigDecimal.valueOf(req.getLat()) : BigDecimal.ZERO);
            route.setEndLng(req.getLng() != null ? BigDecimal.valueOf(req.getLng()) : BigDecimal.ZERO);
            route.setEndLat(req.getLat() != null ? BigDecimal.valueOf(req.getLat()) : BigDecimal.ZERO);
            route.setStatus(2); // 2 = recording
            route.setIsPublic(0);
            route.setViewCount(0);
            route = routeRepo.save(route);
        }
        RouteWaypoint wp = new RouteWaypoint();
        wp.setRouteId(route.getId());
        wp.setName(req.getName());
        wp.setLng(req.getLng() != null ? BigDecimal.valueOf(req.getLng()) : BigDecimal.ZERO);
        wp.setLat(req.getLat() != null ? BigDecimal.valueOf(req.getLat()) : BigDecimal.ZERO);
        wp.setPointType(req.getType() != null ? req.getType() : "custom");
        wp.setDescription(req.getNote());
        wp.setPhotoUrl(req.getPhotoUrl());
        wp.setDayNumber(req.getDayNumber() != null ? req.getDayNumber() : 1);
        wp.setSortOrder(waypointRepo.findByRouteIdOrderByDayNumberAscSortOrderAsc(route.getId()).size() + 1);
        wp = waypointRepo.save(wp);
        return ApiResponse.success(Map.of("routeId", route.getId(), "waypointId", wp.getId()));
    }

    /**
     * Finish recording a trip — set status to published.
     */
    @PostMapping("/{id}/finish")
    public ApiResponse<Void> finishRecording(@PathVariable Long id) {
        return routeRepo.findById(id).map(r -> {
            r.setStatus(1); routeRepo.save(r);
            return ApiResponse.<Void>success(null);
        }).orElse(ApiResponse.error(ErrorCode.NOT_FOUND));
    }

    /**
     * Generate a new route (roadbook) based on user preferences and matched template.
     *
     * @param userId authenticated user ID from JWT interceptor
     * @param req    generation request with trip parameters
     * @return detailed route response
     */
    @PostMapping("/generate")
    public ApiResponse<RouteGenerateResponse> generate(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody RouteGenerateRequest req) {
        try {
            RouteGenerateResponse response = routeGenerateService.generate(userId, req);
            return ApiResponse.success(response);
        } catch (RuntimeException e) {
            if ("TEMPLATE_NOT_FOUND".equals(e.getMessage())) {
                return ApiResponse.error(ErrorCode.TEMPLATE_NOT_FOUND, "暂无可匹配路线，请调整条件后重试");
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
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String keyword) {
        Page<Route> routes = routeService.listByUser(userId, page, size, sortBy, sortDir, keyword);
        return ApiResponse.success(routes);
    }

    /**
     * Update route title/description. Owner only.
     */
    @PutMapping("/{id}")
    public ApiResponse<Route> update(@RequestAttribute("userId") Long userId, @PathVariable Long id, @RequestBody Map<String, String> body) {
        Route route = routeRepo.findById(id).orElseThrow(() -> new RuntimeException("路线不存在"));
        if (!route.getUserId().equals(userId)) return ApiResponse.error(ErrorCode.FORBIDDEN);
        if (body.containsKey("title")) route.setTitle(body.get("title"));
        if (body.containsKey("description")) route.setDescription(body.get("description"));
        return ApiResponse.success(routeRepo.save(route));
    }

    /**
     * Delete a route. Owner only.
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        Route route = routeRepo.findById(id).orElseThrow(() -> new RuntimeException("路线不存在"));
        if (!route.getUserId().equals(userId)) return ApiResponse.error(ErrorCode.FORBIDDEN);
        List<RouteWaypoint> wps = waypointRepo.findByRouteIdOrderByDayNumberAscSortOrderAsc(id);
        waypointRepo.deleteAll(wps);
        routeRepo.delete(route);
        return ApiResponse.success(null);
    }

    /** Publish a route to community. Owner only. */
    @PostMapping("/{id}/publish")
    public ApiResponse<Void> publish(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        Route route = routeRepo.findById(id).orElseThrow();
        if (!route.getUserId().equals(userId)) return ApiResponse.error(ErrorCode.FORBIDDEN);
        route.setIsPublic(1); routeRepo.save(route);
        return ApiResponse.success(null);
    }

    /** Browse community public routes */
    @GetMapping("/community")
    public ApiResponse<List<Route>> community(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(routeService.listPublic(page, size).getContent());
    }

    /**
     * AI conversational route adjustment.
     * Takes an existing route ID and a user message, regenerates with AI.
     */
    @PostMapping("/{id}/adjust")
    public ApiResponse<?> adjustRoute(@RequestAttribute("userId") Long userId,
                                      @PathVariable Long id,
                                      @RequestBody RouteAdjustRequest req) {
        Route route = routeRepo.findById(id).orElse(null);
        if (route == null) return ApiResponse.error(ErrorCode.NOT_FOUND);
        if (!route.getUserId().equals(userId)) return ApiResponse.error(ErrorCode.FORBIDDEN);

        List<RouteWaypoint> wps = waypointRepo.findByRouteIdOrderByDayNumberAscSortOrderAsc(id);
        // Serialize current route as JSON for AI context
        try {
            String routeJson = objectMapper.writeValueAsString(buildDetailResponse(route, wps));
            AiRouteGenerator.AiRoute adjusted = aiGenerator.adjust(routeJson, req.getMessage());
            if (adjusted == null) return ApiResponse.error(ErrorCode.INTERNAL_ERROR);

            // Delete old waypoints and save new ones
            waypointRepo.deleteAll(wps);
            route.setTitle(adjusted.title());
            route.setDescription(adjusted.description());
            route.setTotalDays(adjusted.totalDays());
            route.setTotalDistance(adjusted.totalDistanceKm());
            routeRepo.save(route);

            List<RouteWaypoint> newWps = new java.util.ArrayList<>();
            for (int i = 0; i < adjusted.waypoints().size(); i++) {
                AiRouteGenerator.AiWaypoint awp = adjusted.waypoints().get(i);
                RouteWaypoint wp = new RouteWaypoint();
                wp.setRouteId(id);
                wp.setSortOrder(i + 1);
                wp.setDayNumber(awp.day());
                wp.setPointType(awp.type() != null ? awp.type() : "scenic");
                wp.setName(awp.name());
                wp.setDescription(awp.tips());
                wp.setStayDuration(awp.stayMin());
                wp.setLng(java.math.BigDecimal.ZERO);
                wp.setLat(java.math.BigDecimal.ZERO);
                newWps.add(wp);
            }
            waypointRepo.saveAll(newWps);

            // Geocode + fetch distances for new waypoints (async-like best effort)
            routeGenerateService.enrichWaypoints(id, newWps);

            return ApiResponse.success(buildDetailResponse(route, waypointRepo.findByRouteIdOrderByDayNumberAscSortOrderAsc(id)));
        } catch (Exception e) {
            log.error("Route adjustment failed for route {}: {}", id, e.getMessage(), e);
            return ApiResponse.error(ErrorCode.INTERNAL_ERROR);
        }
    }

    // ========== Private helpers ==========

    /**
     * Build a RouteDetailResponse from stored Route and RouteWaypoint entities.
     */
    private RouteDetailResponse buildDetailResponse(Route route, List<RouteWaypoint> waypoints) {
        if (waypoints == null) {
            waypoints = Collections.emptyList();
        }

        // Batch-fetch POI scores for waypoints that reference a POI
        List<Long> poiIds = waypoints.stream()
                .map(RouteWaypoint::getPoiId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, com.roadbook.poi.entity.Poi> poiMap = poiIds.isEmpty()
                ? Collections.emptyMap()
                : poiRepo.findAllById(poiIds).stream()
                    .collect(Collectors.toMap(com.roadbook.poi.entity.Poi::getId, p -> p));

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

                // Look up POI scores
                com.roadbook.poi.entity.Poi poi = rwp.getPoiId() != null ? poiMap.get(rwp.getPoiId()) : null;

                WaypointDetail detail = WaypointDetail.builder()
                        .sort(rwp.getSortOrder())
                        .type(rwp.getPointType())
                        .name(rwp.getName())
                        .description(rwp.getDescription())
                        .photoUrl(rwp.getPhotoUrl())
                        .lng(rwp.getLng())
                        .lat(rwp.getLat())
                        .arrival(rwp.getArrivalTime())
                        .departure(rwp.getDepartureTime())
                        .stayMin(rwp.getStayDuration())
                        .distanceFromPrevKm(distKm > 0 ? Math.round(distKm * 10.0) / 10.0 : 0.0)
                        .isBreak(isBreak)
                        .driveScore(poi != null && poi.getDriveScore() != null ? poi.getDriveScore().intValue() : null)
                        .parkingScore(poi != null && poi.getParkingScore() != null ? poi.getParkingScore().intValue() : null)
                        .roadScore(poi != null && poi.getRoadScore() != null ? poi.getRoadScore().intValue() : null)
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
                .status(route.getStatus())
                .itinerary(itinerary)
                .fuelStops(fuelStops)
                .createdAt(route.getCreatedAt())
                .build();
    }
}
