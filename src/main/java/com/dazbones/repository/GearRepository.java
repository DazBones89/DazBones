package com.dazbones.repository;

import com.dazbones.model.Gear;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GearRepository extends JpaRepository<Gear, Long> {
}