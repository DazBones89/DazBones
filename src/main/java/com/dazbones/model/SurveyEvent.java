package com.dazbones.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "survey_events")
public class SurveyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_date", unique = true, nullable = false)
    private LocalDate targetDate;

    @Column(nullable = false)
    private String title = "参加アンケート";

    @Column(name = "manual_flg")
    private Boolean manualFlg = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void preInsert() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (title == null || title.isBlank()) {
            title = "参加アンケート";
        }
        if (manualFlg == null) {
            manualFlg = false;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public String getTitle() {
        return title;
    }

    public Boolean getManualFlg() {
        return manualFlg;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setManualFlg(Boolean manualFlg) {
        this.manualFlg = manualFlg;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}