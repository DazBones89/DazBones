package com.dazbones.controller;

import com.dazbones.model.UserSession;
import com.dazbones.service.SurveyMemberService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SurveyMemberController {

    private final SurveyMemberService service;

    public SurveyMemberController(SurveyMemberService service) {
        this.service = service;
    }

    @GetMapping("/admin/survey-members")
    public String list(HttpSession session, Model model) {
        if (!canManage(session)) {
            return "error/404";
        }

        model.addAttribute("members", service.getActiveMembers());
        model.addAttribute("userSession", session.getAttribute("userSession"));

        return "admin/surveyMemberList";
    }

    @PostMapping("/admin/survey-members")
    public String add(@RequestParam("name") String name,
                      HttpSession session,
                      RedirectAttributes redirectAttributes) {

        if (!canManage(session)) {
            return "error/404";
        }

        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "名前を入力してください");
            return "redirect:/admin/survey-members";
        }

        if (service.existsByName(name.trim())) {
            redirectAttributes.addFlashAttribute("errorMessage", "同じ名前がすでに登録されています");
            return "redirect:/admin/survey-members";
        }

        service.addMember(name);
        redirectAttributes.addFlashAttribute("successMessage", "回答者を登録しました");

        return "redirect:/admin/survey-members";
    }

    @PostMapping("/admin/survey-members/{id}/delete")
    public String delete(@PathVariable Long id,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return "error/404";
        }

        service.deleteMember(id);
        redirectAttributes.addFlashAttribute("successMessage", "回答者を削除しました");

        return "redirect:/admin/survey-members";
    }

    private boolean canManage(HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("userSession");
        return user != null && user.canManage();
    }

    private boolean isAdmin(HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("userSession");
        return user != null && user.isAdmin();
    }
}