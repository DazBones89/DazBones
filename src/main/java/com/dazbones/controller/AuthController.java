package com.dazbones.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    // ログイン画面表示（すでに PageController で /login を持ってるなら、どちらか片方に統一してください）
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // ログイン処理
    @PostMapping("/login")
    public String doLogin(@RequestParam("code") String code,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {

        // 仕様のコード
        if ("Baseball_988".equals(code)) {
            session.setAttribute("role", "admin");
            redirectAttributes.addFlashAttribute("successMessage", "管理者としてログインしました");
            return "redirect:/main";
        }
        if ("@6639".equals(code)) {
            session.setAttribute("role", "editor");
            redirectAttributes.addFlashAttribute("successMessage", "編集者としてログインしました");
            return "redirect:/main";
        }

        // 失敗
        redirectAttributes.addFlashAttribute("errorMessage", "コードが正しくありません");
        return "redirect:/login";
    }

    // ログアウト
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("successMessage", "ログアウトしました");
        return "redirect:/main";
    }
}