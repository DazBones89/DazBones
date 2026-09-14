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
        return repo.findTop3ByAudienceAndPublishedAtLessThanEqualOrderByPublishedAtDesc("PUBLIC", LocalDateTime.now());
    }

    public List<News> getAllPublished() {
        return published("PUBLIC");
    }

    public List<News> published(String audience) {
        return repo.findByAudienceAndPublishedAtLessThanEqualOrderByPublishedAtDesc(audience,LocalDateTime.now());
    }
    public List<News> memberTop3() {
        return repo.findTop3ByAudienceAndPublishedAtLessThanEqualOrderByPublishedAtDesc("MEMBERS",LocalDateTime.now());
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