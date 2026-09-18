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
    private final com.dazbones.service.AnnualFeeService feeService;
    private final SurveyService surveyService;

    public PageController(NewsService newsService,
                          ScheduleService scheduleService,
                          com.dazbones.service.AnnualFeeService feeService,
                          SurveyService surveyService) {
        this.newsService = newsService;
        this.scheduleService = scheduleService;
        this.feeService = feeService;
        this.surveyService = surveyService;
    }

    @GetMapping({"/", "/main"})
    public String home(Model model, HttpSession session) {
        model.addAttribute("userSession", session.getAttribute("userSession"));
        model.addAttribute("newsList", newsService.visible(session.getAttribute("userSession") != null).stream().limit(3).toList());
        model.addAttribute("memberNewsList", session.getAttribute("userSession") != null ? newsService.memberTop3() : java.util.List.of());
        model.addAttribute("todaySchedules", scheduleService.getToday());

        model.addAttribute("unpaidCount", feeService.unpaidCount());


        return "main";
    }

    @GetMapping("/policy")
    public String policy(Model model, HttpSession session) {
        model.addAttribute("userSession", session.getAttribute("userSession"));
        return "policy";
    }

    @GetMapping("/history")
    public String history(@org.springframework.web.bind.annotation.RequestParam(required=false) Integer year, Model model, HttpSession session) {
        int selected = year == null ? java.time.LocalDate.now().getYear() : year;
        if (selected < 1900 || selected > 2100) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);
        var rows = scheduleService.getRange(java.time.LocalDate.of(selected,1,1),java.time.LocalDate.of(selected+1,1,1)).stream()
                .filter(s -> s.getEventDate().isBefore(java.time.LocalDate.now())).toList();
        model.addAttribute("year", selected);
        var years = new java.util.TreeSet<Integer>(java.util.Comparator.reverseOrder());
        years.add(selected); years.add(java.time.LocalDate.now().getYear());
        scheduleService.getPast().forEach(s -> years.add(s.getEventDate().getYear()));
        model.addAttribute("years", years); model.addAttribute("history", rows);
        model.addAttribute("wins",rows.stream().filter(s -> "勝利".equals(s.getResultStatus())).count());
        model.addAttribute("losses",rows.stream().filter(s -> "敗北".equals(s.getResultStatus())).count());
        model.addAttribute("draws",rows.stream().filter(s -> "引分".equals(s.getResultStatus())).count());
        model.addAttribute("userSession", session.getAttribute("userSession"));
        return "history";
    }

    @GetMapping("/photo")
    public String photo(Model model, HttpSession session) {
        model.addAttribute("userSession", session.getAttribute("userSession"));
        return "photo";
    }

}
