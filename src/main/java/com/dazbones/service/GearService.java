package com.dazbones.service;

import com.dazbones.model.Gear;
import com.dazbones.repository.GearRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GearService {

    private final GearRepository repository;
    private final com.dazbones.repository.PlayerRepository players;

    public GearService(GearRepository repository, com.dazbones.repository.PlayerRepository players) {
        this.repository = repository;
        this.players = players;
    }

    public List<Gear> findAll() {
        return repository.findAll();
    }

    @org.springframework.transaction.annotation.Transactional
    public void save(Gear gear) {
        Gear target = gear.getId() == null ? new Gear() : repository.findById(gear.getId())
                .orElseThrow(() -> new IllegalArgumentException("道具が見つかりません"));
        if (gear.getOwnerId() != null) {
            var owner = players.findById(gear.getOwnerId()).orElseThrow(() -> new IllegalArgumentException("所有者が見つかりません"));
            if (!Integer.valueOf(0).equals(owner.getDeleteFlg()) && !java.util.Objects.equals(target.getOwnerId(), owner.getId()))
                throw new IllegalArgumentException("削除済み選手を新しい所有者に指定できません");
        }
        target.setName(gear.getName().trim());
        target.setOwnerId(gear.getOwnerId());
        target.setComment(gear.getComment());
        repository.saveAndFlush(target);
    }

    public Gear findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
