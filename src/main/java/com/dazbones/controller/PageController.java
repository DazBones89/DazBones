package com.dazbones.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PageController {

    // トップページ（全ロールOK）
    @GetMapping({"/", "/main"})
    public String homePage() {
        return "main";
    }

    // ログインページ（全ロールOK）
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // 管理者コード変更ページ（管理者のみ）
    @GetMapping("/admin/code")
    public String adminCodePage(HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "error/404";
        }
        return "adminCode";
    }

    // 道具管理ページ（管理者・編集者のみ）
    @GetMapping("/gear")
    public String gearPage(HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"admin".equals(role) && !"editor".equals(role)) {
            return "error/404";
        }
        return "gear";
    }

    // 部費管理ページ（管理者・編集者のみ）
    @GetMapping("/fee")
    public String feePage(HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"admin".equals(role) && !"editor".equals(role)) {
            return "error/404";
        }
        return "fee";
    }

    // 大会履歴（全ロールOK）※編集機能は別で制御予定
    @GetMapping("/history")
    public String historyPage() {
        return "history";
    }

    // 選手紹介（全ロールOK）※編集機能は別で制御予定
    @GetMapping("/player")
    public String playerPage(Model model) {
        List<String> players = List.of("山田 太郎", "佐藤 次郎", "鈴木 三郎");
        model.addAttribute("players", players);
        return "player";
    }

    // プライバシーポリシー（全ロールOK）
    @GetMapping("/policy")
    public String policyPage() {
        return "policy";
    }

    // システム設定確認ページ（管理者のみ）
    @GetMapping("/system")
    public String systemInfoPage(HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) {
            return "error/404";
        }
        return "systemInfo";
    }

    // アンケートページ（管理者・編集者のみ）
    @GetMapping("/survey")
    public String surveyPage(HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"admin".equals(role) && !"editor".equals(role)) {
            return "error/404";
        }
        return "survey";
    }

    // テストリンク（開発環境限定：ここでは常時表示）
    @GetMapping("/testlinks")
    public String testLinksPage() {
        return "testlinks";
    }
}