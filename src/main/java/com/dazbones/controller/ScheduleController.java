package com.dazbones.controller;

import com.dazbones.model.Schedule;
import com.dazbones.model.UserSession;
import com.dazbones.repository.ScheduleRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class ScheduleController {

    private final ScheduleRepository scheduleRepository;

    public ScheduleController(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @GetMapping("/schedule")
    public String schedule(Model model, HttpSession session) {

        LocalDate today = LocalDate.now();

        model.addAttribute("futureList",
                scheduleRepository.findByEventDateGreaterThanEqualOrderByEventDateAsc(today));

        model.addAttribute("pastList",
                scheduleRepository.findByEventDateLessThanOrderByEventDateDesc(today));

        model.addAttribute("userSession",
                (UserSession) session.getAttribute("userSession"));

        return "schedule";
    }
}