package com.dazbones.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "attendance_answers", uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "target_date"}))
public class AttendanceAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "player_id", nullable = false)
    private Long playerId;
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;
    @Column(nullable = false, length = 1)
    private String status;
    @Column(nullable = false, length = 500)
    private String memo = "";
    @Version private Long version;
    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long value) { playerId = value; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate value) { targetDate = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getMemo() { return memo; }
    public void setMemo(String value) { memo = value; }
    public Long getVersion() { return version; }
}
