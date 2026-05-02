package com.dazbones.controller;

import com.dazbones.service.FeeService;
import com.dazbones.service.NewsService;
import com.dazbones.service.ScheduleService;
import com.dazbones.service.SurveyService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private final NewsService newsService;
    private final ScheduleService scheduleService;
    private final FeeService feeService;
    private final SurveyService surveyService;

    public PageController(NewsService newsService,
                          ScheduleService scheduleService,
                          FeeService feeService,
                          SurveyService surveyService) {
        this.newsService = newsService;
        this.scheduleService = scheduleService;
        this.feeService = feeService;
        this.surveyService = surveyService;
    }

    @GetMapping({"/", "/main"})
    public String home(Model model, HttpSession session) {
        model.addAttribute("userSession", session.getAttribute("userSession"));
        model.addAttribute("newsList", newsService.getTop3());
        model.addAttribute("todaySchedules", scheduleService.getToday());

        model.addAttribute("unpaidCount", feeService.countUnpaid());
        model.addAttribute("todayNoAnswerCount", surveyService.countTodayNoAnswer());

        return "main";
    }

    @GetMapping("/policy")
    public String policy(Model model, HttpSession session) {
        model.addAttribute("userSession", session.getAttribute("userSession"));
        return "policy";
    }

    @GetMapping("/history")
    public String history(Model model, HttpSession session) {
        model.addAttribute("userSession", session.getAttribute("userSession"));
        return "history";
    }

    @GetMapping("/testlinks")
    public String testlinks(Model model, HttpSession session) {
        model.addAttribute("userSession", session.getAttribute("userSession"));
        return "testlinks";
    }
}