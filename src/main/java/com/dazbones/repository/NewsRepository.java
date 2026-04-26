package com.dazbones.repository;

import com.dazbones.model.News;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NewsRepository extends JpaRepository<News, Long> {

    List<News> findTop3ByPublishedAtLessThanEqualOrderByPublishedAtDesc(LocalDateTime now);

    List<News> findByPublishedAtLessThanEqualOrderByPublishedAtDesc(LocalDateTime now);

    default List<News> findTop3Published(LocalDateTime now) {
        return findTop3ByPublishedAtLessThanEqualOrderByPublishedAtDesc(now);
    }

    default List<News> findAllPublished(LocalDateTime now) {
        return findByPublishedAtLessThanEqualOrderByPublishedAtDesc(now);
    }
}