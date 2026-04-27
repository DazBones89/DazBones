package com.dazbones.repository;

import com.dazbones.model.SurveyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, Long> {

    List<SurveyAnswer> findBySurveyEventId(Long id);

    Optional<SurveyAnswer> findBySurveyEventIdAndSurveyMemberId(Long eventId, Long memberId);

    long countBySurveyEventId(Long id);

    long countBySurveyEventIdAndAnswerStatus(Long id, String status);
}