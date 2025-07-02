package com.dazbones.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PlayerController {

    @GetMapping("/player")
    public String showPlayerPage(Model model) {
        // 仮の選手リスト（今後はDBから取得）
        List<String> players = List.of("山田 太郎", "佐藤 次郎", "鈴木 三郎");
        model.addAttribute("players", players);
        return "player";
    }
}