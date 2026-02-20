package com.dazbones.service;

import com.dazbones.model.Player;
import com.dazbones.repository.PlayerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public List<Player> findAll() {
        return playerRepository.findAll();
    }

    public Player save(Player player) {
        return playerRepository.save(player);
    }

    public Player findById(Integer id) {
        return playerRepository.findById(id).orElse(null);
    }

    public void deleteById(Integer id) {
        playerRepository.deleteById(id);
    }

    public boolean existsById(Integer id) {
        return playerRepository.existsById(id);
    }
}