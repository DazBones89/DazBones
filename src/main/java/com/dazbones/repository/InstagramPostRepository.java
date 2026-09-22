package com.dazbones.repository;
import com.dazbones.model.InstagramPost;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InstagramPostRepository extends JpaRepository<InstagramPost,Long> { boolean existsByShortcode(String shortcode); }
