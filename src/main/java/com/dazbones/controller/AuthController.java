package com.dazbones.controller;

import com.dazbones.model.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @PostMapping("/login")
    public String login(@RequestParam String code, HttpSession session) {
        if ("Baseball_988".equals(code)) {
            session.setAttribute("user", new UserSession("admin"));
        } else if ("@6639".equals(code)) {
            session.setAttribute("user", new UserSession("editor"));
        } else {
            session.setAttribute("user", new UserSession("viewer")); // 任意：不正なコードでもログイン状態
        }
        return "redirect:/main";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/main";
    }
}