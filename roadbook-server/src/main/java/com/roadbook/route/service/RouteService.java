package com.roadbook.route.service;

import com.roadbook.route.entity.Route;
import com.roadbook.route.entity.RouteWaypoint;
import com.roadbook.route.repository.RouteRepository;
import com.roadbook.route.repository.RouteWaypointRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RouteService {

    private final RouteRepository routeRepo;
    private final RouteWaypointRepository waypointRepo;

    public RouteService(RouteRepository routeRepo, RouteWaypointRepository waypointRepo) {
        this.routeRepo = routeRepo;
        this.waypointRepo = waypointRepo;
    }

    /**
     * List routes for a user with optional sorting and keyword search.
     */
    public Page<Route> listByUser(Long userId, int page, int size, String sortBy, String sortDir, String keyword) {
        Sort sort = buildSort(sortBy, sortDir);
        PageRequest pr = PageRequest.of(page, size, sort);
        if (keyword != null && !keyword.isBlank()) {
            return routeRepo.findByUserIdAndTitleContaining(userId, keyword.trim(), pr);
        }
        return routeRepo.findByUserId(userId, pr);
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String field = "createdAt";
        if ("totalDays".equals(sortBy)) field = "totalDays";
        else if ("totalDistance".equals(sortBy)) field = "totalDistance";
        Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field);
    }

    /**
     * Get a single route by ID.
     *
     * @param routeId route ID
     * @return the route, or null if not found
     */
    public Route getById(Long routeId) {
        return routeRepo.findById(routeId).orElse(null);
    }

    /**
     * Get all waypoints for a route, ordered by day and sort order.
     *
     * @param routeId route ID
     * @return ordered list of waypoints
     */
    public List<RouteWaypoint> getWaypoints(Long routeId) {
        return waypointRepo.findByRouteIdOrderByDayNumberAscSortOrderAsc(routeId);
    }
    public Page<Route> listPublic(int page, int size) {
        return routeRepo.findByIsPublicAndStatusOrderByCreatedAtDesc(1, 1, PageRequest.of(page, size));
    }
}
