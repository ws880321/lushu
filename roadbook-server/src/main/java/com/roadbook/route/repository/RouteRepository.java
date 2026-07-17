package com.roadbook.route.repository;

import com.roadbook.route.entity.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    Page<Route> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Route> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId, int status);
}
