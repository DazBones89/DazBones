package com.dazbones.repository;

import com.dazbones.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByEventDateGreaterThanEqualOrderByEventDateAsc(LocalDate today);

    List<Schedule> findByEventDateLessThanOrderByEventDateDesc(LocalDate today);

    List<Schedule> findByEventDateOrderByStartTimeAsc(LocalDate eventDate);
}