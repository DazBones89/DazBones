package com.dazbones.controller;
import com.dazbones.service.SocialService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller public class SocialController {
 private final SocialService social;
 public SocialController(SocialService social){this.social=social;}
 @GetMapping("/sns") public String page(Model m,HttpSession s){m.addAttribute("links",social.links());m.addAttribute("userSession",s.getAttribute("userSession"));return "sns";}
 @PostMapping("/admin/social/add") public String add(@RequestParam String url,RedirectAttributes flash){try{social.add(url);flash.addFlashAttribute("message","SNSを追加しました。");}catch(IllegalArgumentException e){flash.addFlashAttribute("error",e.getMessage());flash.addFlashAttribute("submittedUrl",url);}return "redirect:/sns";}
 @PostMapping("/admin/social/delete") public String delete(@RequestParam String id){social.delete(id);return "redirect:/sns";}
}
