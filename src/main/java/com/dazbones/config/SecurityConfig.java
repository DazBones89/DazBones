package com.dazbones.config;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean
    AuthenticationManager authenticationManager(@Value("${app.auth.admin-code:}") String adminCode,
                                                @Value("${app.auth.editor-code:}") String editorCode) {
        if (!adminCode.isBlank() && adminCode.equals(editorCode)) {
            throw new IllegalStateException("管理者と編集者には異なるログインコードを設定してください");
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        List<UserDetails> users = new ArrayList<>();
        if (!adminCode.isBlank()) {
            users.add(User.withUsername("admin").password(encoder.encode(adminCode)).roles("ADMIN").build());
        }
        if (!editorCode.isBlank()) {
            users.add(User.withUsername("editor").password(encoder.encode(editorCode)).roles("EDITOR").build());
        }
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(new InMemoryUserDetailsManager(users));
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contexts,
                                            CsrfTokenRepository csrfTokens) throws Exception {
        return http
                .securityContext(context -> context.securityContextRepository(contexts))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/admin/survey-members/*/delete", "/players/*/restore", "/gear/delete").hasRole("ADMIN")
                        .requestMatchers("/admin/schedules/**", "/admin/survey-members/**",
                                "/players/add", "/players/*/edit", "/players/*/delete",
                                "/survey/**", "/api/survey/**", "/fee/**", "/gear/**").hasAnyRole("ADMIN", "EDITOR")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            if (request.getRequestURI().startsWith("/api/") || "POST".equals(request.getMethod())) {
                                response.setStatus(401);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"success\":false,\"message\":\"ログインしてください\"}");
                            } else {
                                response.sendRedirect("/login");
                            }
                        })
                        .accessDeniedHandler((request, response, exception) -> response.sendError(403)))
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login")
                        .invalidateHttpSession(true).deleteCookies("JSESSIONID"))
                .build();
    }
}
