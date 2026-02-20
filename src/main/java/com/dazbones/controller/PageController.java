package com.dazbones.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    // トップページ（全ロールOK）
    @GetMapping({"/", "/main"})
    public String homePage() {
        return "main";
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

    // ※ /player は PlayerController に移管（DBから取得して表示）
    // ここには定義しない（URL競合を防ぐ）

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