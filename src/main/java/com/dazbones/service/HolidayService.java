package com.dazbones.service;

import com.dazbones.model.Holiday;
import com.dazbones.repository.HolidayRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Service
public class HolidayService {

    private final HolidayRepository repository;

    public HolidayService(HolidayRepository repository) {
        this.repository = repository;
    }

    public List<Holiday> getAll() {
        return repository.findAllByOrderByHolidayDateAsc();
    }

    public boolean isHoliday(LocalDate date) {
        return repository.existsByHolidayDate(date);
    }

    public void addHoliday(LocalDate date, String name) {
        if (date == null || name == null || name.trim().isEmpty()) {
            return;
        }

        if (repository.existsByHolidayDate(date)) {
            return;
        }

        Holiday holiday = new Holiday();
        holiday.setHolidayDate(date);
        holiday.setName(name.trim());
        repository.save(holiday);
    }

    public void deleteHoliday(Long id) {
        repository.deleteById(id);
    }

    public int importCsv(MultipartFile file) throws Exception {
        int count = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split(",", 2);

                if (parts.length < 2) {
                    continue;
                }

                LocalDate date = LocalDate.parse(parts[0].trim());
                String name = parts[1].trim();

                if (!repository.existsByHolidayDate(date)) {
                    Holiday holiday = new Holiday();
                    holiday.setHolidayDate(date);
                    holiday.setName(name);
                    repository.save(holiday);
                    count++;
                }
            }
        }

        return count;
    }
}