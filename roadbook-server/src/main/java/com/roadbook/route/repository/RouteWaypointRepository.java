package com.roadbook.route.repository;

import com.roadbook.route.entity.RouteWaypoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteWaypointRepository extends JpaRepository<RouteWaypoint, Long> {

    List<RouteWaypoint> findByRouteIdOrderByDayNumberAscSortOrderAsc(Long routeId);
}
