package com.dazbones.controller;

import com.dazbones.model.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {
    private final com.dazbones.service.CredentialService credentials;
    private final SecurityContextRepository contexts;
    private final CsrfTokenRepository csrfTokens;

    public LoginController(com.dazbones.service.CredentialService credentials, SecurityContextRepository contexts,
                           CsrfTokenRepository csrfTokens) {
        this.credentials = credentials;
        this.contexts = contexts;
        this.csrfTokens = csrfTokens;
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        model.addAttribute("userSession", session.getAttribute("userSession"));
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("code") String code, @RequestParam(required=false) String loginId, HttpServletRequest request,
                        HttpServletResponse response, RedirectAttributes redirectAttributes) {
        UserSession user = credentials.authenticate(loginId, code);
        if (user != null) {
            HttpSession previous = request.getSession(false);
            if (previous != null) previous.invalidate();
            request.getSession(true).setAttribute("userSession", user);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user.getLoginId(), null,
                    java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase(java.util.Locale.ROOT)))));
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, request, response);
            csrfTokens.saveToken(null, request, response);
            return "redirect:/main";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "ログインコードが違います");
        return "redirect:/login";
    }
}
