package com.dazbones.service;

import com.dazbones.model.News;
import com.dazbones.repository.NewsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NewsService {

    private final NewsRepository repo;

    public NewsService(NewsRepository repo) {
        this.repo = repo;
    }

    public List<News> getTop3() {
        return repo.findTop3Published(LocalDateTime.now());
    }

    public List<News> getAllPublished() {
        return repo.findAllPublished(LocalDateTime.now());
    }

    public List<News> getAll() {
        return repo.findAll();
    }

    public News findById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public void save(News n) {
        if (n.getId() == null) {
            n.setCreatedAt(LocalDateTime.now());
        }
        n.setUpdatedAt(LocalDateTime.now());
        repo.save(n);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}