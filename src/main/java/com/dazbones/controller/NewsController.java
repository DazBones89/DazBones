package com.dazbones.controller;

import com.dazbones.model.News;
import com.dazbones.model.UserSession;
import com.dazbones.service.NewsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class NewsController {

    private final NewsService service;

    public NewsController(NewsService service) {
        this.service = service;
    }

    @GetMapping("/news")
    public String list(Model model, HttpSession session) {

        model.addAttribute("newsList", service.getAllPublished());
        model.addAttribute("userSession", session.getAttribute("userSession"));

        return "news";
    }

    @GetMapping("/news/{id}")
    public String detail(@PathVariable Long id, Model model, HttpSession session) {

        News news = service.findById(id);

        if (news == null) return "error/404";

        model.addAttribute("news", news);
        model.addAttribute("userSession", session.getAttribute("userSession"));

        return "newsDetail";
    }
}