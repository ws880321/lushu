package com.roadbook.route.service;

import com.roadbook.route.entity.UserFavorite;
import com.roadbook.route.repository.UserFavoriteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class FavoriteService {

    private final UserFavoriteRepository userFavoriteRepository;

    public FavoriteService(UserFavoriteRepository userFavoriteRepository) {
        this.userFavoriteRepository = userFavoriteRepository;
    }

    /**
     * Add a favorite. If the same (userId, favType, targetId) already exists,
     * returns the existing record without creating a duplicate.
     *
     * @param userId   the authenticated user ID
     * @param favType  the favorite type
     * @param targetId the target entity ID
     * @return the created (or existing) favorite
     */
    @Transactional
    public UserFavorite add(Long userId, String favType, Long targetId) {
        Optional<UserFavorite> existing = userFavoriteRepository
                .findByUserIdAndFavTypeAndTargetId(userId, favType, targetId);
        if (existing.isPresent()) {
            return existing.get();
        }

        UserFavorite fav = new UserFavorite();
        fav.setUserId(userId);
        fav.setFavType(favType);
        fav.setTargetId(targetId);

        UserFavorite saved = userFavoriteRepository.save(fav);
        log.info("User {} favorited {}#{}", userId, favType, targetId);
        return saved;
    }

    /**
     * List favorites by type for the user.
     *
     * @param userId  the authenticated user ID
     * @param favType the favorite type to filter by
     * @return list of favorites
     */
    public List<UserFavorite> listByType(Long userId, String favType) {
        return userFavoriteRepository.findByUserIdAndFavType(userId, favType);
    }

    /**
     * Remove a favorite by type and target ID.
     *
     * @param userId   the authenticated user ID
     * @param favType  the favorite type
     * @param targetId the target entity ID
     * @return true if a favorite was removed, false if it did not exist
     */
    @Transactional
    public boolean remove(Long userId, String favType, Long targetId) {
        Optional<UserFavorite> existing = userFavoriteRepository
                .findByUserIdAndFavTypeAndTargetId(userId, favType, targetId);
        if (existing.isEmpty()) {
            return false;
        }

        userFavoriteRepository.delete(existing.get());
        log.info("User {} unfavorited {}#{}", userId, favType, targetId);
        return true;
    }
}
