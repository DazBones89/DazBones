package com.dazbones.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class FeeController {

    @GetMapping("/fee")
    public String showFeePage(Model model) {
        Map<String, Boolean> feeStatus = new LinkedHashMap<>();
        feeStatus.put("山田 太郎", true);
        feeStatus.put("佐藤 次郎", false);
        feeStatus.put("鈴木 三郎", true);

        model.addAttribute("feeStatus", feeStatus);
        return "fee";
    }
}