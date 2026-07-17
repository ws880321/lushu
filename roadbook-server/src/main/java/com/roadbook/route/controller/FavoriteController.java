package com.roadbook.route.controller;

import com.roadbook.common.ApiResponse;
import com.roadbook.common.ErrorCode;
import com.roadbook.route.entity.UserFavorite;
import com.roadbook.route.service.FavoriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /**
     * Add a favorite.
     *
     * @param userId authenticated user ID from JWT interceptor
     * @param body   JSON body with "favType" and "targetId"
     * @return the created (or existing) favorite
     */
    @PostMapping
    public ApiResponse<UserFavorite> add(
            @RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> body) {
        String favType = (String) body.get("favType");
        Object targetIdObj = body.get("targetId");

        if (favType == null || favType.isBlank() || targetIdObj == null) {
            return ApiResponse.error(ErrorCode.BAD_REQUEST, "favType and targetId are required");
        }

        Long targetId;
        try {
            targetId = ((Number) targetIdObj).longValue();
        } catch (ClassCastException e) {
            return ApiResponse.error(ErrorCode.BAD_REQUEST, "targetId must be a number");
        }

        UserFavorite saved = favoriteService.add(userId, favType, targetId);
        return ApiResponse.success(saved);
    }

    /**
     * List favorites by type for the current user.
     *
     * @param userId authenticated user ID from JWT interceptor
     * @param type   the favorite type to filter by
     * @return list of favorites
     */
    @GetMapping
    public ApiResponse<List<UserFavorite>> list(
            @RequestAttribute("userId") Long userId,
            @RequestParam("type") String type) {
        if (type == null || type.isBlank()) {
            return ApiResponse.error(ErrorCode.BAD_REQUEST, "type parameter is required");
        }
        List<UserFavorite> favorites = favoriteService.listByType(userId, type);
        return ApiResponse.success(favorites);
    }

    /**
     * Remove a favorite by type and target ID.
     *
     * @param userId   authenticated user ID from JWT interceptor
     * @param type     the favorite type
     * @param targetId the target entity ID
     * @return success response
     */
    @DeleteMapping
    public ApiResponse<Void> remove(
            @RequestAttribute("userId") Long userId,
            @RequestParam("type") String type,
            @RequestParam("targetId") Long targetId) {
        if (type == null || type.isBlank()) {
            return ApiResponse.error(ErrorCode.BAD_REQUEST, "type parameter is required");
        }

        boolean removed = favoriteService.remove(userId, type, targetId);
        if (!removed) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "Favorite not found");
        }
        return ApiResponse.success(null);
    }
}
