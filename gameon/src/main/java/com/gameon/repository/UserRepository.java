package com.gameon.repository;

import com.gameon.model.entity.User;
import com.gameon.model.enums.UserRole;
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

    boolean existsByEmail(String email);

    List<User> findByUsernameContainingIgnoreCase(String query);

    Page<User> findByUsernameContainingIgnoreCase(String query, Pageable pageable);

    List<User> findByUserRole(UserRole role);

    List<User> findByIsActiveTrue();

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.sportProfiles WHERE u.userId = :userId")
    Optional<User> findByIdWithSportProfiles(@Param("userId") Long userId);

    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.userRole = :role")
    List<User> findActiveUsersByRole(@Param("role") UserRole role);

    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();
}
