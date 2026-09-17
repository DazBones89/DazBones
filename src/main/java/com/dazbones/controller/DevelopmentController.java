package com.dazbones.controller;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;
@Controller @Profile("!prod")
public class DevelopmentController {
    @GetMapping("/testlinks")
    public String testlinks(Model model,HttpSession session){model.addAttribute("userSession",session.getAttribute("userSession"));return "testlinks";}
}
