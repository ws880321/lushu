package com.roadbook.route.service;

import com.roadbook.route.entity.Route;
import com.roadbook.route.entity.RouteWaypoint;
import com.roadbook.route.repository.RouteRepository;
import com.roadbook.route.repository.RouteWaypointRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
     * List routes for a user, ordered by creation time descending.
     *
     * @param userId the user ID
     * @param page   page number (0-based)
     * @param size   page size
     * @return paginated routes
     */
    public Page<Route> listByUser(Long userId, int page, int size) {
        return routeRepo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
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
}
