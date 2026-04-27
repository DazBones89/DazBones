package com.dazbones.controller;

import com.dazbones.model.UserSession;
import com.dazbones.service.NewsService;
import com.dazbones.service.ScheduleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private final NewsService newsService;
    private final ScheduleService scheduleService;

    public PageController(NewsService newsService,
                          ScheduleService scheduleService) {
        this.newsService = newsService;
        this.scheduleService = scheduleService;
    }

    @GetMapping({"/", "/main"})
    public String home(Model model, HttpSession session) {

        model.addAttribute("userSession", session.getAttribute("userSession"));
        model.addAttribute("newsList", newsService.getTop3());
        model.addAttribute("todaySchedules", scheduleService.getToday());

        return "main";
    }
}