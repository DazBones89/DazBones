package com.dazbones.model;

import jakarta.persistence.*;

@Entity
@Table(name = "player_positions")
public class PlayerPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String position;

    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;

    // getter / setter
}