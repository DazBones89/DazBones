package com.dazbones.controller;

import com.dazbones.model.SiteSetting;
import com.dazbones.repository.SiteSettingRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ContactController {
    private final SiteSettingRepository settings;
    public ContactController(SiteSettingRepository settings) { this.settings = settings; }

    @GetMapping("/contact")
    public String page(Model model, HttpSession session) {
        if (!model.containsAttribute("contactText")) {
            model.addAttribute("contactText", settings.findById("contact.text").map(s -> s.value).orElse(""));
        }
        model.addAttribute("userSession", session.getAttribute("userSession"));
        return "contact";
    }

    @PostMapping("/admin/contact")
    public String save(@RequestParam String contactText, RedirectAttributes flash) {
        if (contactText.length() > 5000) {
            flash.addFlashAttribute("error", "連絡先は5000文字以内で入力してください。");
            flash.addFlashAttribute("contactText", contactText);
        } else {
            settings.save(new SiteSetting("contact.text", contactText));
            flash.addFlashAttribute("message", "連絡先を保存しました。");
        }
        return "redirect:/contact";
    }
}
