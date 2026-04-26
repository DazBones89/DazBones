package com.dazbones.controller;

import com.dazbones.model.News;
import com.dazbones.model.UserSession;
import com.dazbones.repository.NewsRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
public class AdminNewsController {

    private final NewsRepository newsRepository;

    public AdminNewsController(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    @GetMapping("/admin/news")
    public String adminNewsList(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "error/404";
        }

        model.addAttribute("newsList", newsRepository.findAll());
        model.addAttribute("userSession", getUserSession(session));
        return "admin/newsList";
    }

    @GetMapping("/admin/news/new")
    public String newNewsPage(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "error/404";
        }

        model.addAttribute("news", new News());
        model.addAttribute("userSession", getUserSession(session));
        return "admin/newsForm";
    }

    @PostMapping("/admin/news")
    public String createNews(@ModelAttribute News news, HttpSession session) {
        if (!isAdmin(session)) {
            return "error/404";
        }

        news.setCreatedAt(LocalDateTime.now());
        news.setUpdatedAt(LocalDateTime.now());
        newsRepository.save(news);

        return "redirect:/admin/news";
    }

    @GetMapping("/admin/news/{id}/edit")
    public String editNewsPage(@PathVariable Long id, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "error/404";
        }

        News news = newsRepository.findById(id).orElse(null);
        if (news == null) {
            return "error/404";
        }

        model.addAttribute("news", news);
        model.addAttribute("userSession", getUserSession(session));
        return "admin/newsForm";
    }

    @PostMapping("/admin/news/{id}/edit")
    public String updateNews(@PathVariable Long id,
                             @ModelAttribute News form,
                             HttpSession session) {
        if (!isAdmin(session)) {
            return "error/404";
        }

        News news = newsRepository.findById(id).orElse(null);
        if (news == null) {
            return "error/404";
        }

        news.setTitle(form.getTitle());
        news.setContent(form.getContent());
        news.setPublishedAt(form.getPublishedAt());
        news.setUpdatedAt(LocalDateTime.now());

        newsRepository.save(news);

        return "redirect:/admin/news";
    }

    @PostMapping("/admin/news/{id}/delete")
    public String deleteNews(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return "error/404";
        }

        newsRepository.deleteById(id);
        return "redirect:/admin/news";
    }

    private UserSession getUserSession(HttpSession session) {
        return (UserSession) session.getAttribute("userSession");
    }

    private boolean isAdmin(HttpSession session) {
        UserSession user = getUserSession(session);
        return user != null && user.isAdmin();
    }
}