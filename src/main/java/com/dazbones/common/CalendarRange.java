package com.dazbones.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public record CalendarRange(LocalDate start, LocalDate end) {
    public static CalendarRange parse(String start, String end) {
        try {
            LocalDate first = date(start);
            LocalDate last = date(end);
            long days = ChronoUnit.DAYS.between(first, last);
            if (days <= 0 || days > 93) throw new IllegalArgumentException();
            return new CalendarRange(first, last);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "日付範囲は1日以上93日以内で指定してください");
        }
    }

    private static LocalDate date(String value) {
        return value.length() == 10 ? LocalDate.parse(value) : OffsetDateTime.parse(value).toLocalDate();
    }
}
