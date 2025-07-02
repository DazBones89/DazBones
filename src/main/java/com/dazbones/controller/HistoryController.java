package com.dazbones.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HistoryController {

    @GetMapping("/history")
    public String showHistoryPage(Model model) {
        // 仮の大会・活動履歴（後でDBから取得）
        List<String> histories = List.of(
                "2024年5月 草野球春季大会 優勝",
                "2024年8月 親善試合 vs XYZチーム 勝利",
                "2025年3月 合宿＠千葉"
        );
        model.addAttribute("histories", histories);
        return "history";
    }
}