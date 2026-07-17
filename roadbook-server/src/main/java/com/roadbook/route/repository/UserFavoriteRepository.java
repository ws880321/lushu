package com.roadbook.route.repository;

import com.roadbook.route.entity.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {

    List<UserFavorite> findByUserIdAndFavType(Long userId, String favType);

    Optional<UserFavorite> findByUserIdAndFavTypeAndTargetId(Long userId, String favType, Long targetId);
}
