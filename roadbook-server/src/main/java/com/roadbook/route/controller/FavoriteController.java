package com.roadbook.route.controller;

import com.roadbook.common.ApiResponse;
import com.roadbook.common.ErrorCode;
import com.roadbook.route.entity.UserFavorite;
import com.roadbook.route.repository.UserFavoriteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/favorites")
public class FavoriteController {

    private final UserFavoriteRepository userFavoriteRepository;

    public FavoriteController(UserFavoriteRepository userFavoriteRepository) {
        this.userFavoriteRepository = userFavoriteRepository;
    }

    /**
     * Add a favorite. If the same (userId, favType, targetId) already exists,
     * returns the existing record without creating a duplicate.
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

        // Check for existing favorite to avoid duplicates
        Optional<UserFavorite> existing = userFavoriteRepository
                .findByUserIdAndFavTypeAndTargetId(userId, favType, targetId);
        if (existing.isPresent()) {
            return ApiResponse.success(existing.get());
        }

        UserFavorite fav = new UserFavorite();
        fav.setUserId(userId);
        fav.setFavType(favType);
        fav.setTargetId(targetId);

        UserFavorite saved = userFavoriteRepository.save(fav);
        log.info("User {} favorited {}#{}", userId, favType, targetId);
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
        List<UserFavorite> favorites = userFavoriteRepository.findByUserIdAndFavType(userId, type);
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
        if (type == null || type.isBlank() || targetId == null) {
            return ApiResponse.error(ErrorCode.BAD_REQUEST, "type and targetId are required");
        }

        Optional<UserFavorite> existing = userFavoriteRepository
                .findByUserIdAndFavTypeAndTargetId(userId, type, targetId);
        if (existing.isEmpty()) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "Favorite not found");
        }

        userFavoriteRepository.delete(existing.get());
        log.info("User {} unfavorited {}#{}", userId, type, targetId);
        return ApiResponse.success(null);
    }
}
