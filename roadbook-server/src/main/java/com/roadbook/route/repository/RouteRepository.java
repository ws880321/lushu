package com.roadbook.route.repository;

import com.roadbook.route.entity.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    Page<Route> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT r FROM Route r WHERE r.userId = :userId AND LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Route> findByUserIdAndTitleContaining(@Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);

    Optional<Route> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId, int status);
    Page<Route> findByIsPublicAndStatusOrderByCreatedAtDesc(Integer isPublic, Integer status, Pageable pageable);
}
