package com.gameon.repository;

import com.gameon.model.entity.Sport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportRepository extends JpaRepository<Sport, Long> {

    Optional<Sport> findBySportName(String sportName);

    boolean existsBySportName(String sportName);

    @Query("SELECT s FROM Sport s WHERE s.sportId NOT IN " +
           "(SELECT usp.sport.sportId FROM UserSportProfile usp WHERE usp.user.userId = :userId)")
    List<Sport> findSportsNotOnUserProfile(@Param("userId") Long userId);
}
