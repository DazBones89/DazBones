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
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contexts;
    private final CsrfTokenRepository csrfTokens;

    public LoginController(AuthenticationManager authenticationManager, SecurityContextRepository contexts,
                           CsrfTokenRepository csrfTokens) {
        this.authenticationManager = authenticationManager;
        this.contexts = contexts;
        this.csrfTokens = csrfTokens;
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        model.addAttribute("userSession", session.getAttribute("userSession"));
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("code") String code, HttpServletRequest request,
                        HttpServletResponse response, RedirectAttributes redirectAttributes) {
        if (!code.isBlank() && code.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72) {
            for (String role : new String[]{"admin", "editor"}) {
                Authentication authentication;
                try {
                    authentication = authenticationManager.authenticate(
                            UsernamePasswordAuthenticationToken.unauthenticated(role, code));
                } catch (AuthenticationException ignored) {
                    continue;
                }
                HttpSession previous = request.getSession(false);
                if (previous != null) previous.invalidate();
                request.getSession(true).setAttribute("userSession", new UserSession(role));
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
                contexts.saveContext(context, request, response);
                csrfTokens.saveToken(null, request, response);
                return "redirect:/main";
            }
        }
        redirectAttributes.addFlashAttribute("errorMessage", "ログインコードが違います");
        return "redirect:/login";
    }
}
