package com.dazbones.model;

import jakarta.persistence.*;

@Entity
@Table(name = "survey_answers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"surveyEventId","surveyMemberId"}))
public class SurveyAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long surveyEventId;
    private Long surveyMemberId;

    private String answerStatus; // 参加 / 不参加 / 未定
    private String comment;

    // getter setter
    public Long getId() { return id; }
    public Long getSurveyEventId() { return surveyEventId; }
    public void setSurveyEventId(Long surveyEventId) { this.surveyEventId = surveyEventId; }
    public Long getSurveyMemberId() { return surveyMemberId; }
    public void setSurveyMemberId(Long surveyMemberId) { this.surveyMemberId = surveyMemberId; }
    public String getAnswerStatus() { return answerStatus; }
    public void setAnswerStatus(String answerStatus) { this.answerStatus = answerStatus; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}