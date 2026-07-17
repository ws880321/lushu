package com.roadbook.template.repository;

import com.roadbook.template.entity.TemplateWaypoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemplateWaypointRepository extends JpaRepository<TemplateWaypoint, Long> {

    List<TemplateWaypoint> findByTemplateIdOrderByDayNumberAscSortOrderAsc(Long templateId);

    void deleteByTemplateId(Long templateId);
}
