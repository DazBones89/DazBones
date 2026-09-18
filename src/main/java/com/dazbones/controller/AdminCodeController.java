package com.dazbones.controller;
import com.dazbones.service.CredentialService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
public class AdminCodeController {
    private final CredentialService credentials;
    public AdminCodeController(CredentialService credentials){this.credentials=credentials;}
    @GetMapping("/admin/code")
    public String page(HttpSession session,Model model){
        model.addAttribute("userSession",session.getAttribute("userSession")); return "adminCode";
    }
    @PostMapping("/admin/code")
    public String change(@RequestParam(defaultValue="master") String role,@RequestParam String currentCode,@RequestParam String newCode,@RequestParam String confirmation,
                         HttpSession session,Model model,RedirectAttributes flash){
        try { credentials.changeCode(role,currentCode,newCode,confirmation); }
        catch(IllegalArgumentException e){model.addAttribute("errorMessage",e.getMessage()); return page(session,model);}
        session.invalidate(); SecurityContextHolder.clearContext();
        flash.addFlashAttribute("successMessage","コードを変更し、すべてのログインを解除しました。新しいコードでログインしてください。");
        return "redirect:/login";
    }
}
