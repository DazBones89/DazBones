package com.dazbones.repository;

import com.dazbones.model.AttendanceAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceAnswerRepository extends JpaRepository<AttendanceAnswer, Long> {
    List<AttendanceAnswer> findByTargetDateBetweenOrderByTargetDateAsc(LocalDate start, LocalDate end);
    Optional<AttendanceAnswer> findByPlayerIdAndTargetDate(Long playerId, LocalDate targetDate);
}
