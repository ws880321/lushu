package com.roadbook.poi.controller;

import com.roadbook.common.ApiResponse;
import com.roadbook.common.ErrorCode;
import com.roadbook.poi.entity.Poi;
import com.roadbook.poi.service.PoiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/pois")
public class PoiAdminController {

    private final PoiService poiService;

    public PoiAdminController(PoiService poiService) {
        this.poiService = poiService;
    }

    /**
     * Paginated list of POIs with optional filters.
     *
     * @param page     page number (0-based, default 0)
     * @param size     page size (default 20)
     * @param category optional category filter
     * @param province optional province filter
     * @param name     optional name search
     * @return paginated POI list
     */
    @GetMapping
    public ApiResponse<Page<Poi>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String name) {
        Page<Poi> result = poiService.findAll(category, province, name, page, size);
        return ApiResponse.success(result);
    }

    /**
     * Get a single POI by ID.
     *
     * @param id POI ID
     * @return POI detail
     */
    @GetMapping("/{id}")
    public ApiResponse<Poi> getById(@PathVariable Long id) {
        Poi poi = poiService.findById(id);
        if (poi == null) {
            return ApiResponse.error(ErrorCode.NOT_FOUND);
        }
        return ApiResponse.success(poi);
    }

    /**
     * Create a new POI.
     *
     * @param poi POI data
     * @return created POI
     */
    @PostMapping
    public ApiResponse<Poi> create(@RequestBody Poi poi) {
        if (poi.getName() == null || poi.getName().isBlank()) {
            return ApiResponse.error(ErrorCode.BAD_REQUEST, "名称不能为空");
        }
        if (poi.getCategory() == null || poi.getCategory().isBlank()) {
            return ApiResponse.error(ErrorCode.BAD_REQUEST, "类别不能为空");
        }
        if (poi.getLng() == null || poi.getLat() == null) {
            return ApiResponse.error(ErrorCode.BAD_REQUEST, "坐标不能为空");
        }
        // Set defaults for admin-created POIs
        poi.setSource("manual");
        poi.setConfirmedCount(0);
        if (poi.getCampingAllowed() == null) poi.setCampingAllowed(0);
        if (poi.getPetFriendly() == null) poi.setPetFriendly(0);
        Poi created = poiService.create(poi);
        log.info("Admin created POI: id={}, name={}", created.getId(), created.getName());
        return ApiResponse.success(created);
    }

    /**
     * Update an existing POI.
     *
     * @param id  POI ID
     * @param poi updated POI data
     * @return updated POI
     */
    @PutMapping("/{id}")
    public ApiResponse<Poi> update(@PathVariable Long id, @RequestBody Poi poi) {
        Poi updated = poiService.update(id, poi);
        if (updated == null) {
            return ApiResponse.error(ErrorCode.NOT_FOUND);
        }
        log.info("Admin updated POI: id={}", id);
        return ApiResponse.success(updated);
    }

    /**
     * Delete a POI by ID.
     *
     * @param id POI ID
     * @return success response
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        boolean deleted = poiService.delete(id);
        if (!deleted) {
            return ApiResponse.error(ErrorCode.NOT_FOUND);
        }
        log.info("Admin deleted POI: id={}", id);
        return ApiResponse.success(null);
    }
}
