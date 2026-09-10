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

        model.addAttribute("userSession", session.getAttribute("userSession"));

        return "schedule";
    }

    @GetMapping("/api/schedules")
    @ResponseBody
    public List<Map<String, Object>> events(@RequestParam String start, @RequestParam String end) {
        com.dazbones.common.CalendarRange range = com.dazbones.common.CalendarRange.parse(start, end);
        return service.getRange(range.start(), range.end()).stream().map(s -> {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("id", s.getId());
            event.put("title", s.getTitle());
            event.put("start", s.getStartTime() == null ? s.getEventDate().toString()
                    : s.getEventDate().atTime(s.getStartTime()).toString());
            event.put("allDay", s.getStartTime() == null);
            if (s.getStartTime() != null && s.getEndTime() != null) {
                event.put("end", s.getEventDate().atTime(s.getEndTime()).toString());
            }
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("eventDate", s.getEventDate());
            details.put("startTime", s.getStartTime());
            details.put("endTime", s.getEndTime());
            details.put("eventType", s.getEventType());
            details.put("opponent", s.getOpponent());
            details.put("location", s.getLocation());
            details.put("resultStatus", s.getResultStatus());
            details.put("score", s.getScore());
            details.put("comment", s.getComment());
            event.put("extendedProps", details);
            return event;
        }).toList();
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

        applyForm(s, form);
        service.save(s);

        return "redirect:/admin/schedules";
    }

    @GetMapping("/admin/schedules/{id}/edit")
    public String edit(@PathVariable Long id, Model model, HttpSession session) {
        if (!canManage(session)) return "error/404";
        Schedule s = requireSchedule(id);
        ScheduleForm form = new ScheduleForm();
        form.setId(id);
        form.setTitle(s.getTitle());
        form.setEventDate(s.getEventDate());
        form.setStartTime(s.getStartTime());
        form.setEndTime(s.getEndTime());
        form.setLocation(s.getLocation());
        form.setOpponent(s.getOpponent());
        form.setEventType(s.getEventType());
        form.setResultStatus(s.getResultStatus());
        form.setScore(s.getScore());
        form.setComment(s.getComment());
        model.addAttribute("scheduleForm", form);
        model.addAttribute("userSession", session.getAttribute("userSession"));
        return "admin/scheduleForm";
    }

    @PostMapping("/admin/schedules/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute ScheduleForm form,
                         BindingResult result, HttpSession session, Model model) {
        if (!canManage(session)) return "error/404";
        Schedule s = requireSchedule(id);
        form.setId(id);
        if (result.hasErrors()) {
            model.addAttribute("userSession", session.getAttribute("userSession"));
            return "admin/scheduleForm";
        }
        applyForm(s, form);
        service.save(s);
        return "redirect:/admin/schedules";
    }

    @PostMapping("/admin/schedules/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session) {
        if (!canManage(session)) return "error/404";
        requireSchedule(id);
        service.delete(id);
        return "redirect:/admin/schedules";
    }

    private Schedule requireSchedule(Long id) {
        Schedule s = service.findById(id);
        if (s == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        return s;
    }

    private void applyForm(Schedule s, ScheduleForm form) {
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

    }

    private boolean canManage(HttpSession session) {
        UserSession u = (UserSession) session.getAttribute("userSession");
        return u != null && u.canManage();
    }
}
