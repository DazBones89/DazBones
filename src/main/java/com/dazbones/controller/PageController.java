package com.dazbones.controller;

import com.dazbones.model.UserSession;
import com.dazbones.repository.NewsRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;

@Controller
public class PageController {

    private final NewsRepository newsRepository;

    public PageController(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    @GetMapping({"/", "/main"})
    public String homePage(Model model, HttpSession session) {
        model.addAttribute("userSession", getUserSession(session));
        model.addAttribute("newsList", newsRepository.findTop3Published(LocalDateTime.now()));
        return "main";
    }

    @GetMapping("/photo")
    public String photoPage(Model model, HttpSession session) {
        model.addAttribute("userSession", getUserSession(session));
        return "photo";
    }

    @GetMapping("/policy")
    public String policyPage(Model model, HttpSession session) {
        model.addAttribute("userSession", getUserSession(session));
        return "policy";
    }

    @GetMapping("/history")
    public String historyPage(Model model, HttpSession session) {
        model.addAttribute("userSession", getUserSession(session));
        return "history";
    }

    @GetMapping("/testlinks")
    public String testLinksPage(Model model, HttpSession session) {
        model.addAttribute("userSession", getUserSession(session));
        return "testlinks";
    }

    @GetMapping("/survey")
    public String surveyPage(HttpSession session, Model model) {
        if (!canManage(session)) {
            return "error/404";
        }
        model.addAttribute("userSession", getUserSession(session));
        return "survey";
    }

    @GetMapping("/gear")
    public String gearPage(HttpSession session, Model model) {
        if (!canManage(session)) {
            return "error/404";
        }
        model.addAttribute("userSession", getUserSession(session));
        return "gear";
    }

    @GetMapping("/fee")
    public String feePage(HttpSession session, Model model) {
        if (!canManage(session)) {
            return "error/404";
        }
        model.addAttribute("userSession", getUserSession(session));
        return "fee";
    }

    @GetMapping("/system")
    public String systemInfoPage(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "error/404";
        }
        model.addAttribute("userSession", getUserSession(session));
        return "systemInfo";
    }

    @GetMapping("/admin/code")
    public String adminCodePage(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "error/404";
        }
        model.addAttribute("userSession", getUserSession(session));
        return "adminCode";
    }

    private UserSession getUserSession(HttpSession session) {
        return (UserSession) session.getAttribute("userSession");
    }

    private boolean isAdmin(HttpSession session) {
        UserSession user = getUserSession(session);
        return user != null && user.isAdmin();
    }

    private boolean canManage(HttpSession session) {
        UserSession user = getUserSession(session);
        return user != null && user.canManage();
    }
}