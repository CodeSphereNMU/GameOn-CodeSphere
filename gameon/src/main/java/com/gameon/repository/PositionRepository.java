package com.gameon.repository;

import com.gameon.model.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    Optional<Position> findByPositionName(String positionName);

    boolean existsByPositionName(String positionName);
}
