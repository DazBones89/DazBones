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
        return repo.findAll(org.springframework.data.domain.Sort.by("eventDate", "startTime", "id"));
    }

    public List<Schedule> getRange(LocalDate start, LocalDate end) {
        return repo.findByEventDateGreaterThanEqualAndEventDateLessThanOrderByEventDateAscStartTimeAsc(start, end);
    }

    public Schedule findById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public void save(Schedule s) {
        if (s.getId() == null) {
            s.setCreatedAt(LocalDateTime.now());
        }
        s.setUpdatedAt(LocalDateTime.now());
        repo.saveAndFlush(s);
    }

    @org.springframework.transaction.annotation.Transactional
    public void delete(Long id, Long version) {
        Schedule schedule = repo.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        if (!java.util.Objects.equals(version, schedule.getVersion())) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT);
        repo.delete(schedule);
        repo.flush();
    }
}
