package com.roadbook.template.controller;

import com.roadbook.common.ApiResponse;
import com.roadbook.template.entity.RouteTemplate;
import com.roadbook.template.entity.TemplateWaypoint;
import com.roadbook.template.service.TemplateService;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * Search templates by region and/or days. Returns popular templates when no params given.
     *
     * @param region region keyword (optional)
     * @param days   number of trip days (optional)
     * @param tags   comma-separated preferred tags (optional)
     * @return matching templates, or popular when no criteria
     */
    @GetMapping
    public ApiResponse<List<RouteTemplate>> search(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) String tags) {

        // Parse tags
        List<String> tagList = parseTags(tags);

        if (region == null && days == null && tagList.isEmpty()) {
            // No criteria: return popular
            return ApiResponse.success(templateService.listPopular(6));
        }

        int tripDays = (days != null) ? days : 3;
        String searchRegion = (region != null && !region.trim().isEmpty()) ? region.trim() : null;

        Optional<RouteTemplate> matched = templateService.match(searchRegion, tripDays, tagList);
        if (matched.isPresent()) {
            return ApiResponse.success(Collections.singletonList(matched.get()));
        }

        return ApiResponse.success(Collections.emptyList());
    }

    /**
     * Get popular templates for the generate page.
     *
     * @param limit max number of templates (default 6)
     * @return list of popular RouteTemplate
     */
    @GetMapping("/popular")
    public ApiResponse<List<RouteTemplate>> popular(
            @RequestParam(required = false, defaultValue = "6") int limit) {
        int validLimit = Math.max(1, limit);
        return ApiResponse.success(templateService.listPopular(validLimit));
    }

    /**
     * Get waypoints for a specific template.
     *
     * @param id template ID
     * @return ordered list of TemplateWaypoint
     */
    @GetMapping("/{id}/waypoints")
    public ApiResponse<List<TemplateWaypoint>> getWaypoints(@PathVariable Long id) {
        List<TemplateWaypoint> waypoints = templateService.getWaypoints(id);
        if (waypoints.isEmpty()) {
            // Still 200 with empty list — waypoints may simply not exist;
            // the /{id} resource existence check is handled separately
        }
        return ApiResponse.success(waypoints);
    }

    /**
     * Increment usage count (used when a user generates a roadbook from this template).
     *
     * @param id template ID
     * @return success response
     */
    @PostMapping("/{id}/use")
    public ApiResponse<Void> markUsed(@PathVariable Long id) {
        boolean existed = templateService.incrementUsage(id);
        if (!existed) {
            return ApiResponse.error(com.roadbook.common.ErrorCode.TEMPLATE_NOT_FOUND);
        }
        return ApiResponse.success(null);
    }

    // ========== Private helpers ==========

    private List<String> parseTags(String tagsParam) {
        if (tagsParam == null || tagsParam.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = tagsParam.split(",");
        List<String> result = new java.util.ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
