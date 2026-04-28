package com.dazbones.repository;

import com.dazbones.model.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    boolean existsByHolidayDate(LocalDate holidayDate);

    List<Holiday> findAllByOrderByHolidayDateAsc();
}