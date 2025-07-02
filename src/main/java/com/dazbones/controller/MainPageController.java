package com.dazbones.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainPageController {

    @GetMapping("/")
    public String showMainPage(HttpSession session, Model model) {
        // ログイン状態の判定（セッション属性「role」によって）
        String role = (String) session.getAttribute("role");

        if (role != null) {
            if (role.equals("admin")) {
                model.addAttribute("LoginStatus", "管理者");
            } else if (role.equals("editor")) {
                model.addAttribute("LoginStatus", "ログイン中");
            }
        } else {
            model.addAttribute("LoginStatus", ""); // 閲覧者
        }

        return "main"; // templates/main.html を表示
    }
}