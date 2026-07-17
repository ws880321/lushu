package com.roadbook.template.repository;

import com.roadbook.template.entity.RouteTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemplateRepository extends JpaRepository<RouteTemplate, Long> {

    List<RouteTemplate> findByRegionAndTotalDaysAndStatus(String region, Integer totalDays, Integer status);

    List<RouteTemplate> findByRegionAndStatusAndTotalDaysBetween(String region, Integer status, Integer minDays, Integer maxDays);

    /**
     * Find popular templates by usage count descending.
     * Use Pageable to limit results efficiently at the query level.
     */
    List<RouteTemplate> findByStatusOrderByUsageCountDesc(Integer status, Pageable pageable);

    /**
     * Find templates whose region contains the given string, paginated.
     */
    Page<RouteTemplate> findByRegionContaining(String region, Pageable pageable);
}
