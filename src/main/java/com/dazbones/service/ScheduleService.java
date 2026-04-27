package com.dazbones.service;

import com.dazbones.model.Schedule;
import com.dazbones.repository.ScheduleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduleService {

    private final ScheduleRepository repo;

    public ScheduleService(ScheduleRepository repo) {
        this.repo = repo;
    }

    public List<Schedule> getFuture() {
        return repo.findByEventDateGreaterThanEqualOrderByEventDateAsc(LocalDate.now());
    }

    public List<Schedule> getPast() {
        return repo.findByEventDateLessThanOrderByEventDateDesc(LocalDate.now());
    }

    public List<Schedule> getToday() {
        return repo.findByEventDateOrderByStartTimeAsc(LocalDate.now());
    }

    public List<Schedule> getAll() {
        return repo.findAll();
    }

    public Schedule findById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public void save(Schedule s) {
        if (s.getId() == null) {
            s.setCreatedAt(LocalDateTime.now());
        }
        s.setUpdatedAt(LocalDateTime.now());
        repo.save(s);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}