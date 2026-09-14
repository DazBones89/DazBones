package com.dazbones.repository;

import com.dazbones.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Player p where p.id=:id")
    java.util.Optional<Player> lockForFee(Long id);

    // 通常一覧（論理削除除外）
    @Query("""
        SELECT p FROM Player p
        WHERE p.deleteFlg = 0
        ORDER BY
          CASE WHEN p.backNumber IS NULL THEN 1 ELSE 0 END,
          p.backNumber ASC,
          p.createdAt ASC
    """)
    @EntityGraph(attributePaths = "positions")
    List<Player> findActivePlayers();

    // 管理者用（全件）
    @EntityGraph(attributePaths = "positions")
    List<Player> findAll();

    @Override
    @EntityGraph(attributePaths = "positions")
    java.util.Optional<Player> findById(Long id);
}
