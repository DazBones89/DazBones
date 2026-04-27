package com.dazbones.repository;

import com.dazbones.model.SurveyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface SurveyEventRepository extends JpaRepository<SurveyEvent, Long> {
    Optional<SurveyEvent> findByTargetDate(LocalDate date);
}