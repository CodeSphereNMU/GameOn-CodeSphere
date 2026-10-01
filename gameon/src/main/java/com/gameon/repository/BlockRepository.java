package com.gameon.repository;

import com.gameon.model.entity.Block;
import com.gameon.model.entity.BlockId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Block}. Blocks are stored directionally
 * (blocker -> blocked); mutual enforcement is handled in {@code BlockService}.
 */
@Repository
public interface BlockRepository extends JpaRepository<Block, BlockId> {

    /** Directional: does blockerId currently block blockedId? */
    boolean existsByIdBlockerUserIdAndIdBlockedUserId(Long blockerId, Long blockedId);

    /**
     * True if either user has blocked the other (BLOCK(a,b) OR BLOCK(b,a)).
     * The central mutual check, resolved in a single query.
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Block b " +
           "WHERE (b.blocker.userId = :userId1 AND b.blocked.userId = :userId2) " +
           "OR (b.blocker.userId = :userId2 AND b.blocked.userId = :userId1)")
    boolean existsBlockEitherWay(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /** Ids of users that the given user has blocked (outgoing). */
    @Query("SELECT b.blocked.userId FROM Block b WHERE b.blocker.userId = :userId")
    List<Long> findBlockedUserIds(@Param("userId") Long userId);

    /** Ids of users that have blocked the given user (incoming). */
    @Query("SELECT b.blocker.userId FROM Block b WHERE b.blocked.userId = :userId")
    List<Long> findBlockerUserIds(@Param("userId") Long userId);

    /**
     * The full "block wall" for a user: every user id involved in a block with the given
     * user in either direction (outgoing or incoming). This is the id list used to filter
     * feeds, listings, search, etc.
     */
    @Query("SELECT b.blocked.userId FROM Block b WHERE b.blocker.userId = :userId " +
           "UNION " +
           "SELECT b.blocker.userId FROM Block b WHERE b.blocked.userId = :userId")
    List<Long> findAllInvolvedUserIds(@Param("userId") Long userId);

    List<Block> findByIdBlockerUserId(Long blockerId);

    @Modifying
    @Query("DELETE FROM Block b WHERE b.id.blockerUserId = :blockerId AND b.id.blockedUserId = :blockedId")
    void deleteByBlockerAndBlocked(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);
}
