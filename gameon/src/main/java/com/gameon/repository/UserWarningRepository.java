package com.gameon.repository;

import com.gameon.model.entity.UserWarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link UserWarning} - the permanent strike audit records.
 * Records are never deleted; this repository only reads and appends.
 */
@Repository
public interface UserWarningRepository extends JpaRepository<UserWarning, Long> {

    /** Strike history for a single user, newest first (profile + admin per-user history). */
    List<UserWarning> findByUserUserIdOrderByWarningDateDescCreatedAtDesc(Long userId);

    /** All warnings across all users, newest first (admin dashboard / readable view parity). */
    List<UserWarning> findAllByOrderByWarningDateDescCreatedAtDesc();

    /** Total number of strikes ever issued to a user (audit count, independent of resets). */
    long countByUserUserId(Long userId);
}
