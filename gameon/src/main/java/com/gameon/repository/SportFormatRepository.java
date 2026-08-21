package com.gameon.repository;

import com.gameon.model.entity.SportFormat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportFormatRepository extends JpaRepository<SportFormat, Long> {

    @Query("SELECT sf FROM SportFormat sf " +
           "WHERE sf.sport.sportId = :sportId " +
           "ORDER BY sf.noPlayers ASC, sf.formatName ASC, sf.formatId ASC")
    List<SportFormat> findBySportSportId(@Param("sportId") Long sportId);

    @Query("SELECT sf FROM SportFormat sf LEFT JOIN FETCH sf.positions WHERE sf.formatId = :formatId")
    Optional<SportFormat> findByIdWithPositions(@Param("formatId") Long formatId);

    @Query("SELECT sf FROM SportFormat sf " +
           "WHERE sf.sport.sportId IN :sportIds " +
           "ORDER BY sf.sport.sportName ASC, sf.noPlayers ASC, sf.formatName ASC, sf.formatId ASC")
    List<SportFormat> findBySportIds(@Param("sportIds") List<Long> sportIds);

    @Query("SELECT sf FROM SportFormat sf WHERE sf.sport.sportId IN " +
           "(SELECT usp.sport.sportId FROM UserSportProfile usp WHERE usp.user.userId = :userId) " +
           "ORDER BY sf.sport.sportName ASC, sf.noPlayers ASC, sf.formatName ASC, sf.formatId ASC")
    List<SportFormat> findFormatsForUserSports(@Param("userId") Long userId);
}
