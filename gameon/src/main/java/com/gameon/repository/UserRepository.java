package com.gameon.repository;

import com.gameon.model.entity.User;
import com.gameon.model.enums.UserRole;
import com.gameon.model.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByUsernameContainingIgnoreCase(String query);

    Page<User> findByUsernameContainingIgnoreCase(String query, Pageable pageable);

    List<User> findByTypeOfUser(UserRole role);

    List<User> findByAccountStatus(AccountStatus accountStatus);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.sportProfiles WHERE u.userId = :userId")
    Optional<User> findByIdWithSportProfiles(@Param("userId") Long userId);

    @Query("SELECT u FROM User u WHERE u.accountStatus = :status AND u.typeOfUser = :role")
    List<User> findActiveUsersByRole(@Param("role") UserRole role,
                                     @Param("status") AccountStatus status);

    @Query("SELECT COUNT(u) FROM User u WHERE u.accountStatus = :status")
    long countActiveUsers(@Param("status") AccountStatus status);

    @Query("SELECT u FROM User u WHERE u.accountStatus = :status AND LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> searchActiveUsersByUsername(@Param("query") String query,
                                           @Param("status") AccountStatus status,
                                           Pageable pageable);

    /**
     * Batch projection of (userId, profilePictureUrl) for a set of users. Only rows with a
     * non-null picture are returned, so it stays small. Used to attach author avatars to a
     * page of feed DTOs in a single query (no per-post N+1 lookups, no entity loading).
     */
    @Query("SELECT u.userId, u.profilePictureUrl FROM User u " +
           "WHERE u.userId IN :userIds AND u.profilePictureUrl IS NOT NULL")
    List<Object[]> findProfilePictureUrlsByUserIds(@Param("userIds") List<Long> userIds);
}
