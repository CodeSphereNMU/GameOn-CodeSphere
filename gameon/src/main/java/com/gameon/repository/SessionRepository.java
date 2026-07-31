package com.gameon.repository;

import com.gameon.model.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByGameListingGameListingId(Long gameListingId);

    boolean existsByGameListingGameListingId(Long gameListingId);

    @Query("SELECT s FROM Session s WHERE s.sessionDate BETWEEN :start AND :end")
    List<Session> findUpcomingSessions(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
