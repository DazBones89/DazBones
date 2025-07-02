package com.dazbones.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class GearController {

    @GetMapping("/gear")
    public String showGearPage(Model model) {
        // 仮の道具リストと所有者データ
        List<String> players = List.of("山田 太郎", "佐藤 次郎", "鈴木 三郎");
        Map<String, String> gearOwnerMap = Map.of(
                "バット", "山田 太郎",
                "グローブ", "佐藤 次郎",
                "ボール", ""
        );

        model.addAttribute("players", players);
        model.addAttribute("gearOwnerMap", gearOwnerMap);

        return "gear";
    }
}