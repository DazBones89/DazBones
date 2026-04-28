package com.dazbones.controller;

import com.dazbones.model.UserSession;
import com.dazbones.service.SurveyService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Controller
public class SurveyController {

    private final SurveyService surveyService;

    public SurveyController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @GetMapping("/survey")
    public String surveyPage(HttpSession session, Model model) {
        if (!canManage(session)) {
            return "error/404";
        }

        model.addAttribute("members", surveyService.getActiveMembers());
        model.addAttribute("selectedSurveyMemberId", session.getAttribute("selectedSurveyMemberId"));
        model.addAttribute("userSession", session.getAttribute("userSession"));

        return "survey";
    }

    @GetMapping("/api/survey/events")
    @ResponseBody
    public List<Map<String, Object>> getSurveyEvents(@RequestParam String start,
                                                     @RequestParam String end,
                                                     HttpSession session) {
        if (!canManage(session)) {
            return List.of();
        }

        LocalDate current = LocalDate.parse(start);
        LocalDate last = LocalDate.parse(end).minusDays(1);

        List<Map<String, Object>> result = new ArrayList<>();

        while (!current.isAfter(last)) {
            Map<String, Object> summary = surveyService.getSummary(current);
            String type = (String) summary.get("type");

            if (!"none".equals(type)) {
                Map<String, Object> countEvent = new HashMap<>();
                countEvent.put("start", current.toString());
                countEvent.put("title",
                        summary.get("title") + " / "
                                + "参:" + summary.get("参加")
                                + " 不:" + summary.get("不参加")
                                + " 未:" + summary.get("未定")
                                + " 未回:" + summary.get("未回答"));
                countEvent.put("color", "#3b2416");
                result.add(countEvent);
            }

            current = current.plusDays(1);
        }

        return result;
    }

    @GetMapping("/api/survey/day-types")
    @ResponseBody
    public Map<String, String> getSurveyDayTypes(@RequestParam String start,
                                                 @RequestParam String end,
                                                 HttpSession session) {
        if (!canManage(session)) {
            return Map.of();
        }

        LocalDate current = LocalDate.parse(start);
        LocalDate last = LocalDate.parse(end).minusDays(1);

        Map<String, String> result = new HashMap<>();

        while (!current.isAfter(last)) {
            Map<String, Object> summary = surveyService.getSummary(current);
            result.put(current.toString(), (String) summary.get("type"));
            current = current.plusDays(1);
        }

        return result;
    }

    @GetMapping("/api/survey/detail")
    @ResponseBody
    public Map<String, Object> getSurveyDetail(@RequestParam String date,
                                               @RequestParam(required = false) Long memberId,
                                               HttpSession session) {
        if (!canManage(session)) {
            return Map.of();
        }

        return surveyService.getDetail(LocalDate.parse(date), memberId);
    }

    @PostMapping("/survey/answer")
    @ResponseBody
    public Map<String, Object> answer(@RequestParam String date,
                                      @RequestParam Long memberId,
                                      @RequestParam String status,
                                      @RequestParam(required = false) String comment,
                                      HttpSession session) {
        if (!canManage(session)) {
            return Map.of("success", false, "message", "権限がありません");
        }

        surveyService.saveAnswer(LocalDate.parse(date), memberId, status, comment);
        session.setAttribute("selectedSurveyMemberId", memberId);

        return Map.of("success", true);
    }

    @PostMapping("/survey/manual")
    @ResponseBody
    public Map<String, Object> createManual(@RequestParam String date,
                                            @RequestParam(required = false) String title,
                                            HttpSession session) {
        if (!canManage(session)) {
            return Map.of("success", false, "message", "権限がありません");
        }

        surveyService.createManualSurvey(LocalDate.parse(date), title);

        return Map.of("success", true);
    }

    private boolean canManage(HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("userSession");
        return user != null && user.canManage();
    }
}