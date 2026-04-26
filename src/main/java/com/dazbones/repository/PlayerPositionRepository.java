package com.dazbones.repository;

import com.dazbones.model.PlayerPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerPositionRepository extends JpaRepository<PlayerPosition, Long> {

    List<PlayerPosition> findByPlayerId(Long playerId);
}