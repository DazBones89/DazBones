package com.dazbones.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalTime;

public class ScheduleForm {

    private Long id;
    private Long version;
    public Long getVersion() { return version; }
    public void setVersion(Long value) { version = value; }

    @NotBlank(message = "タイトルは必須です")
    @Size(max = 255, message = "タイトルは255文字以内で入力してください")
    private String title;

    @NotNull(message = "日付は必須です")
    private LocalDate eventDate;

    private LocalTime startTime;
    private LocalTime endTime;

    @Size(max = 255)
    private String location;
    @Size(max = 255)
    private String opponent;
    @Pattern(regexp = "|練習|練習試合|公式戦|その他")
    private String eventType;
    @Pattern(regexp = "|予定|勝利|敗北|引分|中止")
    private String resultStatus;
    @Size(max = 255)
    private String score;
    @Size(max = 255)
    private String comment;

    @AssertTrue(message = "終了時間は開始時間より後にしてください")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOpponent() {
        return opponent;
    }

    public void setOpponent(String opponent) {
        this.opponent = opponent;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
