package com.dazbones.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "back_number")
    private Integer backNumber;

    @Column(name = "throw_hand")
    private String throwHand;

    @Column(name = "bat_hand")
    private String batHand;

    @Column(name = "at_bats")
    private Integer atBats;

    private Integer hits;

    @Column(name = "delete_flg")
    private Integer deleteFlg;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 追加① コメント
    private String comment;

    // 追加② 画像パス
    @Column(name = "image_path")
    private String imagePath;

    // ポジション
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerPosition> positions = new ArrayList<>();

    // ============================
    // Getter / Setter
    // ============================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Integer getDeleteFlg() {
        return deleteFlg;
    }

    public void setDeleteFlg(Integer deleteFlg) {
        this.deleteFlg = deleteFlg;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<PlayerPosition> getPositions() {
        return positions;
    }

    public void setPositions(List<PlayerPosition> positions) {
        this.positions = positions;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    // ============================
    // 表示用ロジック
    // ============================

    // 打率（数値）
    public Double getBattingAverage() {
        if (atBats == null || atBats == 0) return null;
        return (double) hits / atBats;
    }

    // 打率（表示用 .321）
    public String getDisplayAverage() {
        if (atBats == null || atBats == 0) return "---";
        return String.format("%.3f", (double) hits / atBats).substring(1);
    }

    // 投打表示
    public String getThrowBatDisplay() {
        if (throwHand == null || batHand == null) return "";
        return throwHand + "投／" + batHand + "打";
    }
}