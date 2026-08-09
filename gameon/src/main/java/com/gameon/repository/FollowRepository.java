package com.gameon.repository;

import com.gameon.model.entity.Follow;
import com.gameon.model.entity.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    boolean existsByIdFollowerUserIdAndIdFollowedUserId(Long followerId, Long followedId);

    long countByIdFollowedUserId(Long userId);

    long countByIdFollowerUserId(Long userId);

    List<Follow> findByIdFollowerUserId(Long userId);

    List<Follow> findByIdFollowedUserId(Long userId);

    @Query("SELECT f.followed.userId FROM Follow f WHERE f.follower.userId = :userId")
    List<Long> findFollowingUserIds(@Param("userId") Long userId);

    @Query("SELECT f.follower.userId FROM Follow f WHERE f.followed.userId = :userId")
    List<Long> findFollowerUserIds(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Follow f WHERE f.id.followerUserId = :followerId AND f.id.followedUserId = :followedId")
    void deleteByFollowerAndFollowed(@Param("followerId") Long followerId, @Param("followedId") Long followedId);
}
