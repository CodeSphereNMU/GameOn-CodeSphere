package com.gameon.repository;

import com.gameon.model.entity.FormatPosition;
import com.gameon.model.entity.FormatPositionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormatPositionRepository extends JpaRepository<FormatPosition, FormatPositionId> {

    List<FormatPosition> findByIdFormatId(Long formatId);

    @Query("SELECT fp FROM FormatPosition fp JOIN FETCH fp.position WHERE fp.id.formatId = :formatId")
    List<FormatPosition> findByFormatIdWithPositions(@Param("formatId") Long formatId);
}
