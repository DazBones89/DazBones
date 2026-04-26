package com.dazbones.controller;

import com.dazbones.model.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {

    private static final String ADMIN_CODE = "Baseball_988";
    private static final String EDITOR_CODE = "@6639";

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("code") String code,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {

        if (ADMIN_CODE.equals(code)) {
            session.setAttribute("userSession", new UserSession("admin"));
            return "redirect:/main";
        }

        if (EDITOR_CODE.equals(code)) {
            session.setAttribute("userSession", new UserSession("editor"));
            return "redirect:/main";
        }

        redirectAttributes.addFlashAttribute("errorMessage", "パスワードが違います");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/main";
    }
}