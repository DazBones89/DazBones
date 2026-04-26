package com.dazbones.controller;

import com.dazbones.model.News;
import com.dazbones.model.UserSession;
import com.dazbones.repository.NewsRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;

@Controller
public class NewsController {

    private final NewsRepository newsRepository;

    public NewsController(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    @GetMapping("/news")
    public String newsList(Model model, HttpSession session) {
        model.addAttribute("newsList", newsRepository.findAllPublished(LocalDateTime.now()));
        model.addAttribute("userSession", getUserSession(session));
        return "news";
    }

    @GetMapping("/news/{id}")
    public String newsDetail(@PathVariable Long id, Model model, HttpSession session) {
        News news = newsRepository.findById(id).orElse(null);

        if (news == null || news.getPublishedAt().isAfter(LocalDateTime.now())) {
            return "error/404";
        }

        model.addAttribute("news", news);
        model.addAttribute("userSession", getUserSession(session));
        return "newsDetail";
    }

    private UserSession getUserSession(HttpSession session) {
        return (UserSession) session.getAttribute("userSession");
    }
}