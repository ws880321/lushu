package com.roadbook.share.controller;

import com.roadbook.amap.AmapClient;
import com.roadbook.amap.dto.GeoCodeResponse;
import com.roadbook.amap.dto.WeatherResponse;
import com.roadbook.common.ApiResponse;
import com.roadbook.common.ErrorCode;
import com.roadbook.route.entity.Route;
import com.roadbook.route.entity.RouteWaypoint;
import com.roadbook.route.repository.RouteRepository;
import com.roadbook.route.repository.RouteWaypointRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/share")
public class ShareController {
    private final RouteRepository routeRepo;
    private final RouteWaypointRepository waypointRepo;
    private final AmapClient amapClient;

    public ShareController(RouteRepository routeRepo, RouteWaypointRepository waypointRepo, AmapClient amapClient) {
        this.routeRepo = routeRepo;
        this.waypointRepo = waypointRepo;
        this.amapClient = amapClient;
    }

    @GetMapping("/reverse-geo")
    public ApiResponse<Map<String, Object>> reverseGeo(@RequestParam double lng, @RequestParam double lat) {
        try {
            GeoCodeResponse resp = amapClient.regeocode(lng, lat);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("lng", lng); data.put("lat", lat);
            data.put("address", resp.getRegeocode() != null && resp.getRegeocode().getFormattedAddress() != null
                    ? resp.getRegeocode().getFormattedAddress() : (lng + "," + lat));
            return ApiResponse.success(data);
        } catch (Exception e) {
            return ApiResponse.error(com.roadbook.common.ErrorCode.AMAP_API_ERROR);
        }
    }

    @GetMapping("/weather")
    public ApiResponse<Map<String, Object>> weather(@RequestParam String city) {
        try {
            WeatherResponse resp = amapClient.weather(city);
            Map<String, Object> data = new LinkedHashMap<>();
            if (resp.getLives() != null && !resp.getLives().isEmpty()) {
                var live = resp.getLives().get(0);
                data.put("city", live.getCity()); data.put("weather", live.getWeather());
                data.put("temperature", live.getTemperature()); data.put("humidity", live.getHumidity());
                data.put("wind", live.getWinddirection() + " " + live.getWindpower() + "级");
            }
            if (resp.getForecasts() != null && !resp.getForecasts().isEmpty()) {
                var fc = resp.getForecasts().get(0);
                data.put("forecasts", fc.getCasts().stream().map(c -> Map.of(
                    "date", c.getDate(), "dayWeather", c.getDayWeather(), "nightWeather", c.getNightWeather(),
                    "dayTemp", c.getDayTemp(), "nightTemp", c.getNightTemp()
                )).limit(5).toList());
            }
            return ApiResponse.success(data);
        } catch (Exception e) { return ApiResponse.error(ErrorCode.AMAP_API_ERROR); }
    }

    @GetMapping("/routes/{id}")
    public ApiResponse<Map<String, Object>> getSharedRoute(@PathVariable Long id) {
        Route route = routeRepo.findById(id).orElseThrow(() -> new RuntimeException("路线不存在"));
        List<RouteWaypoint> waypoints = waypointRepo.findByRouteIdOrderByDayNumberAscSortOrderAsc(id);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", route.getTitle()); data.put("description", route.getDescription());
        data.put("totalDays", route.getTotalDays()); data.put("totalDistanceKm", route.getTotalDistance());
        data.put("startPoint", route.getStartPoint()); data.put("endPoint", route.getEndPoint());
        data.put("createdAt", route.getCreatedAt());
        Map<Integer, List<Map<String, Object>>> byDay = new LinkedHashMap<>();
        for (RouteWaypoint w : waypoints) {
            Map<String, Object> wp = new LinkedHashMap<>();
            wp.put("name", w.getName()); wp.put("type", w.getPointType());
            wp.put("lng", w.getLng()); wp.put("lat", w.getLat());
            wp.put("arrival", w.getArrivalTime() != null ? w.getArrivalTime().toString() : null);
            wp.put("departure", w.getDepartureTime() != null ? w.getDepartureTime().toString() : null);
            wp.put("tips", w.getDescription()); wp.put("distanceFromPrevKm", w.getDistanceFromPrev());
            wp.put("photoUrl", w.getPhotoUrl());
            byDay.computeIfAbsent(w.getDayNumber(), k -> new ArrayList<>()).add(wp);
        }
        data.put("itinerary", byDay);
        return ApiResponse.success(data);
    }
}
