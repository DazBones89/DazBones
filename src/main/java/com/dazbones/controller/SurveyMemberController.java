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
    private final com.dazbones.service.CredentialService credentials;

    public SurveyMemberController(SurveyMemberService service,com.dazbones.service.CredentialService credentials) {
        this.service = service;
        this.credentials=credentials;
    }

    @GetMapping("/admin/survey-members")
    public String list(HttpSession session, Model model) {
        if (!canManage(session)) {
            return "error/404";
        }

        model.addAttribute("members", isAdmin(session)?service.getAllMembers():service.getActiveMembers());
        model.addAttribute("userSession", session.getAttribute("userSession"));

        return "surveyMemberList";
    }

    @PostMapping("/admin/survey-members")
    public String add(@RequestParam("name") String name,
                      HttpSession session,
                      RedirectAttributes redirectAttributes) {

        if (!canManage(session)) {
            return "error/404";
        }

        if (name == null || name.trim().isEmpty() || name.trim().length()>100) {
            redirectAttributes.addFlashAttribute("errorMessage", "名前は1〜100文字で入力してください");
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

    @PostMapping("/admin/survey-members/{id}/restore")
    public String restore(@PathVariable Long id,HttpSession session,RedirectAttributes flash){
        if(!isAdmin(session))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        service.restore(id);flash.addFlashAttribute("successMessage","回答者を復元しました。必要に応じてログインコードを再発行してください。");return "redirect:/admin/survey-members";
    }
    @PostMapping("/admin/survey-members/{id}/code")
    public String issueCode(@PathVariable Long id,HttpSession session,RedirectAttributes flash){
        if(!isAdmin(session))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        try{flash.addFlashAttribute("issuedCode",credentials.issueMemberCode(id));flash.addFlashAttribute("issuedLoginId","member-"+id);}
        catch(IllegalArgumentException e){flash.addFlashAttribute("errorMessage",e.getMessage());}
        return "redirect:/admin/survey-members";
    }

    private boolean isAdmin(HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("userSession");
        return user != null && user.isAdmin();
    }
}
