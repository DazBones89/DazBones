package com.dazbones.repository;

import com.dazbones.model.Fee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeeRepository extends JpaRepository<Fee, Long> {

    Optional<Fee> findByPlayerId(Long playerId);

    List<Fee> findByPaidFlgOrderByUpdatedAtDesc(Integer paidFlg);

    @org.springframework.data.jpa.repository.Query("""
        select count(p) from Player p where p.deleteFlg = 0
        and not exists (select f.id from Fee f where f.playerId = p.id and f.paidFlg = 1)
        """)
    long countUnpaidActivePlayers();
}
