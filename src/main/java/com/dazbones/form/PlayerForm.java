package com.dazbones.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.List;

public class PlayerForm {
    private Long version;
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    @NotBlank(message = "名前は必須です")
    @Size(max = 100, message = "名前は100文字以内で入力してください")
    private String name;

    @Min(0) @Max(99)
    private Integer backNumber;

    @Pattern(regexp = "|右|左|両")
    private String throwHand;
    @Pattern(regexp = "|右|左|両")
    private String batHand;

    @Min(value = 0, message = "打数は0以上で入力してください")
    private Integer atBats;

    @Min(value = 0, message = "ヒット数は0以上で入力してください")
    private Integer hits;

    @Size(max = 255, message = "コメントは255文字以内で入力してください")
    private String comment;

    @Size(max = 4)
    private List<@Pattern(regexp = "投手|捕手|内野手|外野手") String> positions = new ArrayList<>();

    @AssertTrue(message = "ヒット数は打数以下で入力してください")
    public boolean isHitsValid() { return (hits == null ? 0 : hits) <= (atBats == null ? 0 : atBats); }

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
