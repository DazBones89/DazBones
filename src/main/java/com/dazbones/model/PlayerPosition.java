package com.dazbones.model;

import jakarta.persistence.*;

@Entity
@Table(name = "player_positions")
public class PlayerPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 親（Player）
    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;

    // ポジション名
    @Column(name = "position")
    private String position;

    // ===== Getter / Setter =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
}