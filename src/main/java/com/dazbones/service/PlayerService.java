package com.dazbones.service;

import com.dazbones.model.Player;
import com.dazbones.repository.PlayerRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PlayerService {

    private final PlayerRepository repo;

    public PlayerService(PlayerRepository repo) {
        this.repo = repo;
    }

    public List<Player> getActivePlayers() {
        return repo.findActivePlayers();
    }

    public List<Player> getAll() {
        return repo.findAll();
    }

    public Player findById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public void save(Player p) {
        if (p.getId() == null) {
            p.setCreatedAt(LocalDateTime.now());
            p.setDeleteFlg(0);
        }
        p.setUpdatedAt(LocalDateTime.now());
        repo.save(p);
    }

    public void delete(Long id) {
        Player p = findById(id);
        if (p != null) {
            p.setDeleteFlg(1);
            p.setDeletedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            repo.save(p);
        }
    }

    public void restore(Long id) {
        Player p = findById(id);
        if (p != null) {
            p.setDeleteFlg(0);
            p.setDeletedAt(null);
            p.setUpdatedAt(LocalDateTime.now());
            repo.save(p);
        }
    }
}