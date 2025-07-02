package com.dazbones.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class SurveyController {

    @GetMapping("/survey")
    public String showSurveyPage(Model model) {
        // 仮のスケジュール（日付リスト）
        List<String> schedule = List.of(
                "2025-07-06（日）",
                "2025-07-13（日）",
                "2025-07-20（日）"
        );
        model.addAttribute("schedule", schedule);
        return "survey";
    }
}