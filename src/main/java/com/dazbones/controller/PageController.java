package com.dazbones.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PageController {

    @GetMapping("/")
    public String homePage() {
        return "main"; // ルートURL → main.html を表示
    }

    @GetMapping("/main")
    public String mainPage() {
        return "main";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/admin/code")
    public String adminCodePage() {
        return "adminCode";
    }

    @GetMapping("/gear")
    public String gearPage() {
        return "gear";
    }

    @GetMapping("/fee")
    public String feePage() {
        return "fee";
    }

    @GetMapping("/history")
    public String historyPage() {
        return "history";
    }

    @GetMapping("/player")
    public String playerPage(Model model) {
        // 仮の選手リスト（今後DB連携予定）
        List<String> players = List.of("山田 太郎", "佐藤 次郎", "鈴木 三郎");
        model.addAttribute("players", players);
        return "player";
    }

    @GetMapping("/policy")
    public String policyPage() {
        return "policy";
    }

    @GetMapping("/system")
    public String systemInfoPage() {
        return "systemInfo";
    }

    @GetMapping("/survey")
    public String surveyPage() {
        return "survey";
    }
}
