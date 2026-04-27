package com.dazbones.controller;

import com.dazbones.form.ScheduleForm;
import com.dazbones.model.Schedule;
import com.dazbones.model.UserSession;
import com.dazbones.service.ScheduleService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class ScheduleController {

    private final ScheduleService service;

    public ScheduleController(ScheduleService service) {
        this.service = service;
    }

    @GetMapping("/schedule")
    public String schedule(Model model, HttpSession session) {

        model.addAttribute("futureList", service.getFuture());
        model.addAttribute("pastList", service.getPast());
        model.addAttribute("userSession", session.getAttribute("userSession"));

        return "schedule";
    }

    @GetMapping("/admin/schedules")
    public String list(HttpSession session, Model model) {

        if (!canManage(session)) return "error/404";

        model.addAttribute("list", service.getAll());
        model.addAttribute("userSession", session.getAttribute("userSession"));

        return "admin/scheduleList";
    }

    @GetMapping("/admin/schedules/new")
    public String createPage(HttpSession session, Model model) {

        if (!canManage(session)) return "error/404";

        model.addAttribute("scheduleForm", new ScheduleForm());
        model.addAttribute("userSession", session.getAttribute("userSession"));

        return "admin/scheduleForm";
    }

    @PostMapping("/admin/schedules")
    public String create(@Valid @ModelAttribute ScheduleForm form,
                         BindingResult result,
                         HttpSession session,
                         Model model) {

        if (!canManage(session)) return "error/404";

        if (result.hasErrors()) {
            model.addAttribute("userSession", session.getAttribute("userSession"));
            return "admin/scheduleForm";
        }

        Schedule s = new Schedule();

        s.setTitle(form.getTitle());
        s.setEventDate(form.getEventDate());
        s.setStartTime(form.getStartTime());
        s.setEndTime(form.getEndTime());
        s.setLocation(form.getLocation());
        s.setOpponent(form.getOpponent());
        s.setEventType(form.getEventType());
        s.setResultStatus(form.getResultStatus());
        s.setScore(form.getScore());
        s.setComment(form.getComment());

        service.save(s);

        return "redirect:/admin/schedules";
    }

    private boolean canManage(HttpSession session) {
        UserSession u = (UserSession) session.getAttribute("userSession");
        return u != null && u.canManage();
    }
}