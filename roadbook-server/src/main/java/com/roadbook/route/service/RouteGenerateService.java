package com.roadbook.route.service;

import com.roadbook.amap.AmapClient;
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
    private static final double FUEL_RATE_PER_KM = 0.5;
    private static final int DAY_START_HOUR = 8;
    private static final int DAY_START_MINUTE = 0;
    private static final double MIN_DISTANCE_KM = 50.0;
    private static final double MAX_RANDOM_DISTANCE = 80.0;

    private final TemplateService templateService;
    private final AmapClient amapClient;
    private final RouteRepository routeRepo;
    private final RouteWaypointRepository waypointRepo;

    /**
     * Generate a complete route (roadbook) for the user.
     *
     * @param userId the authenticated user ID
     * @param req    the generation request with trip parameters
     * @return detailed route response grouped by day
     */
    public RouteDetailResponse generate(Long userId, RouteGenerateRequest req) {
        // 1. Reverse geocode start point to get province
        GeoCodeResponse regeo = amapClient.regeocode(
                req.getStartPoint().getLng().doubleValue(),
                req.getStartPoint().getLat().doubleValue());
        String province = extractProvince(regeo);

        // 2. Match template using province, days, and tags
        List<String> tags = req.getPreferences() != null
                ? req.getPreferences().getTags()
                : Collections.emptyList();
        RouteTemplate template = templateService.match(province, req.getTotalDays(), tags)
                .orElseThrow(() -> new RuntimeException("TEMPLATE_NOT_FOUND"));
        templateService.incrementUsage(template.getId());

        // 3. Get template waypoints
        List<TemplateWaypoint> twps = templateService.getWaypoints(template.getId());

        // 4. Create Route entity
        Route route = buildRoute(userId, req, template);
        route = routeRepo.save(route);

        // 5. Copy template waypoints to route waypoints, calculate timeline
        List<RouteWaypoint> waypoints = buildWaypoints(route.getId(), twps, req.getPreferences());
        waypointRepo.saveAll(waypoints);

        // 6. Build and return response
        return buildResponse(route, waypoints, req);
    }

    // ========== Private helpers ==========

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
     * Build route waypoints from template waypoints, calculating arrival/departure times.
     * Days start at 08:00. Each waypoint accumulates stay duration.
     */
    private List<RouteWaypoint> buildWaypoints(Long routeId, List<TemplateWaypoint> twps,
                                                RouteGenerateRequest.Preferences preferences) {
        if (twps == null || twps.isEmpty()) {
            return Collections.emptyList();
        }

        // Group template waypoints by day
        Map<Integer, List<TemplateWaypoint>> dayGroups = twps.stream()
                .collect(Collectors.groupingBy(TemplateWaypoint::getDayNumber, TreeMap::new, Collectors.toList()));

        List<RouteWaypoint> result = new ArrayList<>();
        Random random = new Random();

        for (Map.Entry<Integer, List<TemplateWaypoint>> entry : dayGroups.entrySet()) {
            int day = entry.getKey();
            List<TemplateWaypoint> dayWaypoints = entry.getValue();

            // Each day starts at 08:00
            LocalTime currentTime = LocalTime.of(DAY_START_HOUR, DAY_START_MINUTE);

            for (TemplateWaypoint twp : dayWaypoints) {
                RouteWaypoint rwp = new RouteWaypoint();
                rwp.setRouteId(routeId);
                rwp.setDayNumber(day);
                rwp.setSortOrder(twp.getSortOrder());
                rwp.setPointType(twp.getPointType());
                rwp.setName(twp.getName());
                rwp.setLng(twp.getLng());
                rwp.setLat(twp.getLat());
                rwp.setPoiId(twp.getPoiId());

                // Arrival time is the current accumulated time
                rwp.setArrivalTime(currentTime);

                // Calculate stay duration (default 60 minutes for scenic spots, 30 for others)
                int stay = twp.getStayDuration() != null
                        ? twp.getStayDuration()
                        : ("scenic".equals(twp.getPointType()) ? 60 : 30);
                rwp.setStayDuration(stay);

                // Departure time = arrival + stay
                int arrivalMinute = currentTime.getHour() * 60 + currentTime.getMinute();
                int departureMinute = arrivalMinute + stay;
                int depHour = (departureMinute / 60) % 24;
                int depMin = departureMinute % 60;
                rwp.setDepartureTime(LocalTime.of(depHour, depMin));

                // Advance current time for next waypoint (arrival only, travel time handled separately)
                currentTime = rwp.getDepartureTime();

                // Estimate distance from previous waypoint (simplified: random between 50-130 km)
                int distKm = (int) (MIN_DISTANCE_KM + random.nextDouble() * MAX_RANDOM_DISTANCE);
                rwp.setDistanceFromPrev(distKm * 1000); // stored in meters

                // Mark gas/charging points as break points
                String type = twp.getPointType() != null ? twp.getPointType() : "";
                if ("gas".equals(type) || "charging".equals(type) || "service".equals(type)) {
                    rwp.setIsBreakPoint(1);
                }

                // Description from template tips
                rwp.setDescription(twp.getTips());

                result.add(rwp);
            }
        }

        return result;
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
