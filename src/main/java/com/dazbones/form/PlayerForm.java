package com.dazbones.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

public class PlayerForm {

    @NotBlank(message = "名前は必須です")
    private String name;

    private Integer backNumber;

    private String throwHand;
    private String batHand;

    @Min(value = 0, message = "打席数は0以上で入力してください")
    private Integer atBats;

    @Min(value = 0, message = "ヒット数は0以上で入力してください")
    private Integer hits;

    private String comment;

    private List<String> positions = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getBackNumber() {
        return backNumber;
    }

    public void setBackNumber(Integer backNumber) {
        this.backNumber = backNumber;
    }

    public String getThrowHand() {
        return throwHand;
    }

    public void setThrowHand(String throwHand) {
        this.throwHand = throwHand;
    }

    public String getBatHand() {
        return batHand;
    }

    public void setBatHand(String batHand) {
        this.batHand = batHand;
    }

    public Integer getAtBats() {
        return atBats;
    }

    public void setAtBats(Integer atBats) {
        this.atBats = atBats;
    }

    public Integer getHits() {
        return hits;
    }

    public void setHits(Integer hits) {
        this.hits = hits;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<String> getPositions() {
        return positions;
    }

    public void setPositions(List<String> positions) {
        this.positions = positions;
    }
}