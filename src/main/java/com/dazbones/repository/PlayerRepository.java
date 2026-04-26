package com.dazbones.repository;

import com.dazbones.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    // 通常一覧（論理削除除外）
    @Query("""
        SELECT p FROM Player p
        WHERE p.deleteFlg = 0
        ORDER BY
          CASE WHEN p.backNumber IS NULL THEN 1 ELSE 0 END,
          p.backNumber ASC,
          p.createdAt ASC
    """)
    List<Player> findActivePlayers();

    // 管理者用（全件）
    List<Player> findAll();
}