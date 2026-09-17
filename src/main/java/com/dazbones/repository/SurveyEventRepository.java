package com.dazbones.repository;

import com.dazbones.model.SurveyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface SurveyEventRepository extends JpaRepository<SurveyEvent, Long> {
    java.util.List<SurveyEvent> findByTargetDateGreaterThanEqualAndTargetDateLessThan(LocalDate start, LocalDate end);
    Optional<SurveyEvent> findByTargetDate(LocalDate date);
}