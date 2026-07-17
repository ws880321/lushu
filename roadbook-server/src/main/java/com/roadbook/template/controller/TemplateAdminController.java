package com.roadbook.template.controller;

import com.roadbook.common.ApiResponse;
import com.roadbook.common.ErrorCode;
import com.roadbook.template.entity.RouteTemplate;
import com.roadbook.template.entity.TemplateWaypoint;
import com.roadbook.template.service.TemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/templates")
public class TemplateAdminController {

    private final TemplateService templateService;

    public TemplateAdminController(TemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * Paginated list of templates with optional region search.
     *
     * @param page   page number (0-based, default 0)
     * @param size   page size (default 20)
     * @param region optional region filter (fuzzy match)
     * @return paginated template list
     */
    @GetMapping
    public ApiResponse<Page<RouteTemplate>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String region) {
        Page<RouteTemplate> result = templateService.findAll(region, page, size);
        return ApiResponse.success(result);
    }

    /**
     * Get a single template with its waypoints.
     *
     * @param id template ID
     * @return template with waypoints list included
     */
    @GetMapping("/{id}")
    public ApiResponse<TemplateDetail> getById(@PathVariable Long id) {
        RouteTemplate template = templateService.findById(id);
        if (template == null) {
            return ApiResponse.error(ErrorCode.NOT_FOUND);
        }
        TemplateDetail detail = new TemplateDetail(template, templateService.getWaypoints(id));
        return ApiResponse.success(detail);
    }

    /**
     * Create a new template.
     *
     * @param template template data
     * @return created template
     */
    @PostMapping
    public ApiResponse<RouteTemplate> create(@RequestBody RouteTemplate template) {
        if (template.getName() == null || template.getName().isBlank()) {
            return ApiResponse.error(ErrorCode.BAD_REQUEST, "名称不能为空");
        }
        if (template.getRegion() == null || template.getRegion().isBlank()) {
            return ApiResponse.error(ErrorCode.BAD_REQUEST, "区域不能为空");
        }
        if (template.getTotalDays() == null || template.getTotalDays() < 1) {
            return ApiResponse.error(ErrorCode.BAD_REQUEST, "天数必须大于0");
        }
        RouteTemplate created = templateService.create(template);
        log.info("Admin created template: id={}, name={}", created.getId(), created.getName());
        return ApiResponse.success(created);
    }

    /**
     * Update an existing template.
     *
     * @param id       template ID
     * @param template updated template data
     * @return updated template
     */
    @PutMapping("/{id}")
    public ApiResponse<RouteTemplate> update(@PathVariable Long id, @RequestBody RouteTemplate template) {
        RouteTemplate updated = templateService.update(id, template);
        if (updated == null) {
            return ApiResponse.error(ErrorCode.NOT_FOUND);
        }
        log.info("Admin updated template: id={}", id);
        return ApiResponse.success(updated);
    }

    /**
     * Delete a template (also deletes its waypoints).
     *
     * @param id template ID
     * @return success response
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        boolean deleted = templateService.delete(id);
        if (!deleted) {
            return ApiResponse.error(ErrorCode.NOT_FOUND);
        }
        log.info("Admin deleted template: id={}", id);
        return ApiResponse.success(null);
    }

    /**
     * Add a waypoint to a template.
     *
     * @param id       template ID
     * @param waypoint waypoint data
     * @return created waypoint
     */
    @PostMapping("/{id}/waypoints")
    public ApiResponse<TemplateWaypoint> addWaypoint(@PathVariable Long id, @RequestBody TemplateWaypoint waypoint) {
        RouteTemplate template = templateService.findById(id);
        if (template == null) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "模板不存在");
        }
        if (waypoint.getName() == null || waypoint.getName().isBlank()) {
            return ApiResponse.error(ErrorCode.BAD_REQUEST, "途经点名称不能为空");
        }
        if (waypoint.getLng() == null || waypoint.getLat() == null) {
            return ApiResponse.error(ErrorCode.BAD_REQUEST, "坐标不能为空");
        }
        waypoint.setTemplateId(id);
        TemplateWaypoint created = templateService.addWaypoint(waypoint);
        log.info("Admin added waypoint: id={}, name={}", created.getId(), created.getName());
        return ApiResponse.success(created);
    }

    /**
     * Update a waypoint.
     *
     * @param id   template ID (not used in logic, kept for path consistency)
     * @param wpId waypoint ID
     * @param waypoint updated waypoint data
     * @return updated waypoint
     */
    @PutMapping("/{id}/waypoints/{wpId}")
    public ApiResponse<TemplateWaypoint> updateWaypoint(
            @PathVariable Long id,
            @PathVariable Long wpId,
            @RequestBody TemplateWaypoint waypoint) {
        TemplateWaypoint updated = templateService.updateWaypoint(wpId, waypoint);
        if (updated == null) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "途经点不存在");
        }
        log.info("Admin updated waypoint: id={}", wpId);
        return ApiResponse.success(updated);
    }

    /**
     * Delete a waypoint.
     *
     * @param id   template ID (not used in logic, kept for path consistency)
     * @param wpId waypoint ID
     * @return success response
     */
    @DeleteMapping("/{id}/waypoints/{wpId}")
    public ApiResponse<Void> deleteWaypoint(@PathVariable Long id, @PathVariable Long wpId) {
        boolean deleted = templateService.deleteWaypoint(wpId);
        if (!deleted) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "途经点不存在");
        }
        log.info("Admin deleted waypoint: id={}", wpId);
        return ApiResponse.success(null);
    }

    /**
     * DTO that bundles a template with its waypoints for the detail endpoint.
     */
    public record TemplateDetail(RouteTemplate template, java.util.List<TemplateWaypoint> waypoints) {}
}
