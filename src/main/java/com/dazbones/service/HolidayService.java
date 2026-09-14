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
        if (date == null || name == null || name.isBlank() || name.trim().length() > 100)
            throw new IllegalArgumentException("日付と100文字以内の祝日名を入力してください");

        if (repository.existsByHolidayDate(date)) {
            throw new IllegalArgumentException("その日付はすでに登録されています");
        }

        Holiday holiday = new Holiday();
        holiday.setHolidayDate(date);
        holiday.setName(name.trim());
        repository.save(holiday);
    }

    public void deleteHoliday(Long id) {
        repository.deleteById(id);
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public int importCsv(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty() || file.getSize() > 5L * 1024 * 1024)
            throw new IllegalArgumentException("5MB以内のCSVファイルを選択してください");
        java.util.Map<LocalDate, String> rows = new java.util.LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1) line = line.replace("\uFEFF", "");
                if (lineNumber == 1 && (line.equals("date,name") || line.equals("日付,祝日名"))) continue;
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split(",", 2);

                if (parts.length < 2) {
                    throw new IllegalArgumentException(lineNumber + "行目：日付,祝日名の形式で入力してください");
                }

                LocalDate date;
                try { date = LocalDate.parse(parts[0].trim()); }
                catch (java.time.format.DateTimeParseException e) { throw new IllegalArgumentException(lineNumber + "行目：日付はyyyy-MM-dd形式で入力してください"); }
                String name = parts[1].trim();
                if (name.isBlank() || name.length() > 100) throw new IllegalArgumentException(lineNumber + "行目：祝日名は1〜100文字です");
                if (rows.putIfAbsent(date, name) != null) throw new IllegalArgumentException(lineNumber + "行目：CSV内で日付が重複しています");
            }
        }
        if (rows.isEmpty()) throw new IllegalArgumentException("登録する行がありません");
        java.util.List<Holiday> additions = new java.util.ArrayList<>();
        for (var row : rows.entrySet()) {
            if (repository.existsByHolidayDate(row.getKey())) continue;
            Holiday holiday = new Holiday();
            holiday.setHolidayDate(row.getKey());
            holiday.setName(row.getValue());
            additions.add(holiday);
        }
        repository.saveAllAndFlush(additions);
        return additions.size();
    }
}
