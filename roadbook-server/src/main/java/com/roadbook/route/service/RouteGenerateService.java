package com.roadbook.route.service;

import com.roadbook.ai.AiRouteGenerator;
import com.roadbook.amap.AmapClient;
import com.roadbook.amap.dto.DrivingRouteResponse;
import com.roadbook.amap.dto.GeoCodeResponse;
import com.roadbook.route.dto.RouteDetailResponse;
import com.roadbook.route.dto.RouteDetailResponse.DayItinerary;
import com.roadbook.route.dto.RouteDetailResponse.EstimatedCost;
import com.roadbook.route.dto.RouteDetailResponse.FuelStop;
import com.roadbook.route.dto.RouteDetailResponse.WaypointDetail;
import com.roadbook.route.dto.RouteGenerateRequest;
import com.roadbook.route.entity.Route;
import com.roadbook.route.entity.RouteWaypoint;
import com.roadbook.route.repository.RouteRepository;
import com.roadbook.route.repository.RouteWaypointRepository;
import com.roadbook.template.entity.RouteTemplate;
import com.roadbook.template.entity.TemplateWaypoint;
import com.roadbook.template.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RouteGenerateService {

    private static final double TOLL_RATE_PER_KM = 0.15;
    private static final double FUEL_RATE_PER_KM = 0.50;
    private static final int DAY_START_HOUR = 8;
    private static final int DAY_START_MINUTE = 0;
    private static final double AVG_SPEED_KMH = 60.0;
    private static final double FALLBACK_DIST_KM = 30.0;

    private final TemplateService templateService;
    private final AmapClient amapClient;
    private final RouteRepository routeRepo;
    private final RouteWaypointRepository waypointRepo;
    private final AiRouteGenerator aiGenerator;

    /**
     * Generate a complete route (roadbook) for the user.
     *
     * @param userId the authenticated user ID
     * @param req    the generation request with trip parameters
     * @return detailed route response grouped by day
     */
    public RouteDetailResponse generate(Long userId, RouteGenerateRequest req) {
        // 0. Resolve coordinates if only address/name is provided
        resolveCoordinates(req.getStartPoint());
        resolveCoordinates(req.getEndPoint());

        // 1. Reverse geocode start point to get province
        GeoCodeResponse regeo = amapClient.regeocode(
                req.getStartPoint().getLng().doubleValue(),
                req.getStartPoint().getLat().doubleValue());
        String province = extractProvince(regeo);

        // 2. Try template match first, then AI fallback
        List<String> tags = req.getPreferences() != null ? req.getPreferences().getTags() : Collections.emptyList();
        RouteTemplate template = templateService.match(province, req.getTotalDays(), tags).orElse(null);

        List<RouteWaypoint> waypoints;
        Route route;

        boolean templateValid = template != null && regionMatches(province, template.getRegion());

        if (templateValid) {
            // Template generation
            templateService.incrementUsage(template.getId());
            List<TemplateWaypoint> twps = templateService.getWaypoints(template.getId());
            route = buildRoute(userId, req, template);
            route = routeRepo.save(route);
            waypoints = buildWaypoints(route.getId(), twps, req.getPreferences());
        } else {
            // AI generation
            if (template != null) log.info("Template region mismatch: {} != {}, using AI", template.getRegion(), province);
            AiRouteGenerator.AiRoute aiRoute = aiGenerator.generate(
                req.getStartPoint().getName(), req.getEndPoint().getName(),
                req.getTotalDays(), tags,
                req.getPreferences() != null ? req.getPreferences().getDifficulty() : "medium",
                req.getPreferences() != null && req.getPreferences().getDailyDriveHours() != null
                    ? req.getPreferences().getDailyDriveHours() : 4.0);
            if (aiRoute == null) {
                throw new RuntimeException("TEMPLATE_NOT_FOUND");
            }
            route = buildRouteFromAi(userId, req, aiRoute);
            route = routeRepo.save(route);
            waypoints = buildWaypointsFromAi(route.getId(), aiRoute);
        }

        waypointRepo.saveAll(waypoints);
        return buildResponse(route, waypoints, req);
    }

    // ========== Private helpers ==========

    private static final Map<String, String> PROVINCE_TO_REGION = Map.ofEntries(
        Map.entry("四川省", "川西"), Map.entry("云南省", "云南"), Map.entry("贵州省", "贵州"),
        Map.entry("甘肃省", "西北"), Map.entry("青海省", "西北"), Map.entry("宁夏", "西北"),
        Map.entry("安徽省", "华东"), Map.entry("浙江省", "华东"),
        Map.entry("陕西省", "西北")
    );

    private boolean regionMatches(String province, String templateRegion) {
        if (province == null || templateRegion == null) return false;
        if (templateRegion.equals(province)) return true;
        return templateRegion.equals(PROVINCE_TO_REGION.get(province));
    }

    /**
     * If point has address text but no coordinates, geocode it first.
     */
    private void resolveCoordinates(RouteGenerateRequest.PointInfo point) {
        if (point.getLng() != null && point.getLat() != null) return;
        String query = point.getAddress() != null ? point.getAddress() : point.getName();
        if (query == null) return;
        try {
            GeoCodeResponse resp = amapClient.geocode(query);
            if (resp != null && resp.getGeocodes() != null && !resp.getGeocodes().isEmpty()) {
                String[] parts = resp.getGeocodes().get(0).getLocation().split(",");
                point.setLng(new BigDecimal(parts[0]));
                point.setLat(new BigDecimal(parts[1]));
                if (resp.getGeocodes().get(0).getCity() != null) {
                    point.setName(point.getName() + " (" + resp.getGeocodes().get(0).getCity() + ")");
                }
                return;
            }
        } catch (Exception e) {
            log.warn("Geocode failed for: {}", query, e);
        }
        // Fallback: use hardcoded coords for known cities
        if (point.getLng() == null) point.setLng(new BigDecimal("104.0657")); // Chengdu default
        if (point.getLat() == null) point.setLat(new BigDecimal("30.6574"));
    }

    /**
     * Extract province name from the reverse geocode response.
     */
    private String extractProvince(GeoCodeResponse regeo) {
        if (regeo == null) {
            return null;
        }
        if (regeo.getRegeocode() != null
                && regeo.getRegeocode().getAddressComponent() != null
                && regeo.getRegeocode().getAddressComponent().getProvince() != null) {
            return regeo.getRegeocode().getAddressComponent().getProvince();
        }
        if (regeo.getGeocodes() != null && !regeo.getGeocodes().isEmpty()) {
            return regeo.getGeocodes().get(0).getProvince();
        }
        return null;
    }

    /**
     * Build the Route entity from request and matched template.
     */
    private Route buildRouteFromAi(Long userId, RouteGenerateRequest req, AiRouteGenerator.AiRoute aiRoute) {
        Route route = new Route();
        route.setUserId(userId);
        route.setTitle(aiRoute.title());
        route.setDescription(aiRoute.description());
        route.setTotalDays(req.getTotalDays());
        route.setStartPoint(req.getStartPoint().getName());
        route.setEndPoint(req.getEndPoint().getName());
        route.setStartLng(req.getStartPoint().getLng());
        route.setStartLat(req.getStartPoint().getLat());
        route.setEndLng(req.getEndPoint().getLng());
        route.setEndLat(req.getEndPoint().getLat());
        route.setTotalDistance(aiRoute.totalDistanceKm());
        route.setStatus(1);
        route.setIsPublic(0);
        route.setViewCount(0);
        return route;
    }

    private List<RouteWaypoint> buildWaypointsFromAi(Long routeId, AiRouteGenerator.AiRoute aiRoute) {
        List<RouteWaypoint> result = new ArrayList<>();
        int sort = 0;
        for (AiRouteGenerator.AiWaypoint aw : aiRoute.waypoints()) {
            sort++;
            RouteWaypoint rwp = new RouteWaypoint();
            rwp.setRouteId(routeId);
            rwp.setSortOrder(sort);
            rwp.setDayNumber(aw.day());
            rwp.setPointType(aw.type() != null ? aw.type() : "scenic");
            rwp.setName(aw.name());
            rwp.setDescription(aw.tips());
            rwp.setIsBreakPoint("gas".equals(aw.type()) || "charging".equals(aw.type()) ? 1 : 0);
            rwp.setStayDuration(aw.stayMin() > 0 ? aw.stayMin() : 60);
            result.add(rwp);
        }
        // Geocode AI waypoint names, with fallback spread over route extent
        double baseLng = 104, baseLat = 30; // default center
        for (RouteWaypoint rwp : result) {
            try {
                GeoCodeResponse resp = amapClient.geocode(rwp.getName());
                if (resp != null && resp.getGeocodes() != null && !resp.getGeocodes().isEmpty()) {
                    String[] parts = resp.getGeocodes().get(0).getLocation().split(",");
                    rwp.setLng(new BigDecimal(parts[0]));
                    rwp.setLat(new BigDecimal(parts[1]));
                }
            } catch (Exception e) {
                log.debug("Geocode failed for {}, using spread coords", rwp.getName());
            }
            // Always ensure non-null coordinates
            if (rwp.getLng() == null) rwp.setLng(BigDecimal.valueOf(baseLng + Math.random() * 3 - 1.5));
            if (rwp.getLat() == null) rwp.setLat(BigDecimal.valueOf(baseLat + Math.random() * 3 - 1.5));
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }
        // Fill timeline
        LocalTime t = LocalTime.of(8, 0);
        for (RouteWaypoint rwp : result) {
            rwp.setArrivalTime(t);
            int stay = rwp.getStayDuration() != null ? rwp.getStayDuration() : 60;
            rwp.setDepartureTime(t.plusMinutes(stay));
            rwp.setDistanceFromPrev(30000); // 30km fallback per leg
            t = rwp.getDepartureTime().plusMinutes(20); // 20min drive
        }
        return result;
    }

    private Route buildRoute(Long userId, RouteGenerateRequest req, RouteTemplate template) {
        Route route = new Route();
        route.setUserId(userId);
        route.setTitle(template.getName());
        route.setDescription(template.getName());
        route.setTotalDays(req.getTotalDays());
        route.setStartPoint(req.getStartPoint().getName());
        route.setEndPoint(req.getEndPoint().getName());
        route.setStartLng(req.getStartPoint().getLng());
        route.setStartLat(req.getStartPoint().getLat());
        route.setEndLng(req.getEndPoint().getLng());
        route.setEndLat(req.getEndPoint().getLat());

        // Estimate total distance: reasonable default based on template
        route.setTotalDistance(template.getTotalDistance() != null
                ? template.getTotalDistance()
                : req.getTotalDays() * 200);

        route.setTemplateId(template.getId());
        route.setStatus(1);
        route.setIsPublic(0);
        route.setViewCount(0);
        return route;
    }

    /**
     * Build route waypoints from template waypoints, calculating real distances
     * via Amap driving route API and arrival/departure times.
     */
    private List<RouteWaypoint> buildWaypoints(Long routeId, List<TemplateWaypoint> twps,
                                                RouteGenerateRequest.Preferences preferences) {
        if (twps == null || twps.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, List<TemplateWaypoint>> dayGroups = twps.stream()
                .collect(Collectors.groupingBy(TemplateWaypoint::getDayNumber, TreeMap::new, Collectors.toList()));

        List<RouteWaypoint> result = new ArrayList<>();
        double totalMeters = 0;

        for (Map.Entry<Integer, List<TemplateWaypoint>> entry : dayGroups.entrySet()) {
            int day = entry.getKey();
            List<TemplateWaypoint> dayWaypoints = entry.getValue();
            List<Double> dayDistancesM = fetchDayDistances(dayWaypoints);

            LocalTime currentTime = LocalTime.of(DAY_START_HOUR, DAY_START_MINUTE);

            for (int i = 0; i < dayWaypoints.size(); i++) {
                TemplateWaypoint twp = dayWaypoints.get(i);
                double prevDistM = (i > 0) ? dayDistancesM.get(i - 1) : 0;
                totalMeters += prevDistM;

                RouteWaypoint rwp = new RouteWaypoint();
                rwp.setRouteId(routeId);
                rwp.setDayNumber(day);
                rwp.setSortOrder(twp.getSortOrder());
                rwp.setPointType(twp.getPointType());
                rwp.setName(twp.getName());
                rwp.setLng(twp.getLng());
                rwp.setLat(twp.getLat());
                rwp.setPoiId(twp.getPoiId());

                // Add driving time from previous point
                int driveMin = (int) Math.ceil(prevDistM / 1000.0 / AVG_SPEED_KMH * 60);
                int arrivalMin = currentTime.getHour() * 60 + currentTime.getMinute() + driveMin;
                currentTime = LocalTime.of((arrivalMin / 60) % 24, arrivalMin % 60);
                rwp.setArrivalTime(currentTime);

                int stay = twp.getStayDuration() != null ? twp.getStayDuration()
                        : ("scenic".equals(twp.getPointType()) ? 60 : 30);
                rwp.setStayDuration(stay);

                int depMin = currentTime.getHour() * 60 + currentTime.getMinute() + stay;
                rwp.setDepartureTime(LocalTime.of((depMin / 60) % 24, depMin % 60));
                currentTime = rwp.getDepartureTime();

                rwp.setDistanceFromPrev((int) prevDistM);

                String type = twp.getPointType() != null ? twp.getPointType() : "";
                if ("gas".equals(type) || "charging".equals(type) || "service".equals(type)) {
                    rwp.setIsBreakPoint(1);
                }
                rwp.setDescription(twp.getTips());
                result.add(rwp);
            }
        }
        return result;
    }

    /**
     * Fetch real driving distances between consecutive waypoints via Amap API.
     * Falls back to straight-line estimates on API error.
     */
    private List<Double> fetchDayDistances(List<TemplateWaypoint> twps) {
        if (twps.size() <= 1) return Collections.emptyList();

        List<Double> distances = new ArrayList<>();
        for (int i = 1; i < twps.size(); i++) {
            TemplateWaypoint from = twps.get(i - 1);
            TemplateWaypoint to = twps.get(i);
            double distM = FALLBACK_DIST_KM * 1000; // fallback
            try {
                String origin = from.getLng() + "," + from.getLat();
                String dest = to.getLng() + "," + to.getLat();
                DrivingRouteResponse resp = amapClient.drivingRoute(origin, dest, null);
                if (resp != null && resp.getRoute() != null && resp.getRoute().getPaths() != null
                        && !resp.getRoute().getPaths().isEmpty()) {
                    distM = Double.parseDouble(resp.getRoute().getPaths().get(0).getDistance());
                }
            } catch (Exception e) {
                // Estimate from coordinates as fallback
                double dx = (to.getLng().doubleValue() - from.getLng().doubleValue()) * 85390;
                double dy = (to.getLat().doubleValue() - from.getLat().doubleValue()) * 111320;
                distM = Math.sqrt(dx * dx + dy * dy) * 1.4; // road factor
            }
            distances.add(distM);
            // Rate-limit: pause between API calls
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
        return distances;
    }

    /**
     * Build the full response DTO from route and waypoints.
     */
    private RouteDetailResponse buildResponse(Route route, List<RouteWaypoint> waypoints,
                                               RouteGenerateRequest req) {
        if (waypoints == null) {
            waypoints = Collections.emptyList();
        }

        // Group waypoints by day
        Map<Integer, List<RouteWaypoint>> dayGroups = waypoints.stream()
                .collect(Collectors.groupingBy(RouteWaypoint::getDayNumber, TreeMap::new, Collectors.toList()));

        // Build day itineraries
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

                // Estimate drive time: ~60 km/h average speed
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
                        .distanceFromPrevKm(round(distKm, 1))
                        .isBreak(isBreak)
                        .build();
                details.add(detail);

                // Collect fuel/charging stops
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

            // Round day distance
            dayDistanceKm = round(dayDistanceKm, 1);

            DayItinerary dayItem = DayItinerary.builder()
                    .day(day)
                    .distanceKm(dayDistanceKm)
                    .driveTimeMin(driveTimeMin)
                    .summary(buildDaySummary(day, dayWaypoints))
                    .waypoints(details)
                    .build();
            itinerary.add(dayItem);
        }

        // Calculate estimated costs
        EstimatedCost cost = EstimatedCost.builder()
                .tollYuan(round(totalDistanceKm * TOLL_RATE_PER_KM, 2))
                .fuelYuan(round(totalDistanceKm * FUEL_RATE_PER_KM, 2))
                .totalYuan(round(totalDistanceKm * (TOLL_RATE_PER_KM + FUEL_RATE_PER_KM), 2))
                .build();

        totalDistanceKm = round(totalDistanceKm, 1);

        return RouteDetailResponse.builder()
                .routeId(route.getId())
                .title(route.getTitle())
                .description(route.getDescription())
                .totalDays(route.getTotalDays())
                .totalDistanceKm(totalDistanceKm)
                .estimatedCost(cost)
                .itinerary(itinerary)
                .fuelStops(fuelStops)
                .createdAt(route.getCreatedAt())
                .build();
    }

    /**
     * Build a one-line summary for a day's itinerary.
     */
    private String buildDaySummary(int day, List<RouteWaypoint> waypoints) {
        if (waypoints == null || waypoints.isEmpty()) {
            return "第" + day + "天行程";
        }
        String first = waypoints.get(0).getName();
        String last = waypoints.get(waypoints.size() - 1).getName();
        int count = waypoints.size();
        return "第" + day + "天从 " + first + " 出发，途经 " + count + " 个站点，到达 " + last;
    }

    /**
     * Round a double to the specified number of decimal places.
     */
    private double round(double value, int places) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
