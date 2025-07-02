package com.dazbones.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AdminCodeController {

    private String adminCode = "Baseball_988";
    private String editorCode = "@6639";

    @GetMapping("/admin/code")
    public String showCodePage(Model model) {
        model.addAttribute("adminCode", adminCode);
        model.addAttribute("editorCode", editorCode);
        return "adminCode";
    }

    @PostMapping("/admin/code")
    public String updateCodes(
            @RequestParam("newAdminCode") String newAdminCode,
            @RequestParam("newEditorCode") String newEditorCode,
            Model model) {

        this.adminCode = newAdminCode;
        this.editorCode = newEditorCode;

        model.addAttribute("adminCode", adminCode);
        model.addAttribute("editorCode", editorCode);
        model.addAttribute("message", "コードを更新しました！");
        return "adminCode";
    }
}