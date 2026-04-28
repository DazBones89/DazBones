package com.dazbones.repository;

import com.dazbones.model.Fee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeeRepository extends JpaRepository<Fee, Long> {

    Optional<Fee> findByPlayerId(Long playerId);

    List<Fee> findByPaidFlgOrderByUpdatedAtDesc(Integer paidFlg);
}