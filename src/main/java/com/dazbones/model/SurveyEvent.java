package com.dazbones.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "survey_events")
public class SurveyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private LocalDate targetDate;

    private Boolean manualFlg = false;

    // getter setter
    public Long getId() { return id; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public Boolean getManualFlg() { return manualFlg; }
    public void setManualFlg(Boolean manualFlg) { this.manualFlg = manualFlg; }
}