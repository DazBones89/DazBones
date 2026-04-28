package com.dazbones.service;

import com.dazbones.model.Gear;
import com.dazbones.repository.GearRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GearService {

    private final GearRepository repository;

    public GearService(GearRepository repository) {
        this.repository = repository;
    }

    public List<Gear> findAll() {
        return repository.findAll();
    }

    public void save(Gear gear) {
        repository.save(gear);
    }

    public Gear findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}