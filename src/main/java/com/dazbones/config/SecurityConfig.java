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
    org.springframework.security.core.userdetails.UserDetailsService userDetailsService(com.dazbones.repository.LoginCredentialRepository repo) {
        return id -> {
            var c = repo.findById(id).orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("Unknown login"));
            return User.withUsername(c.getLoginId()).password(c.getCodeHash()).roles(c.getRole().toUpperCase(java.util.Locale.ROOT)).build();
        };
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
                                            CsrfTokenRepository csrfTokens, com.dazbones.service.CredentialService credentials) throws Exception {
        return http
                .securityContext(context -> context.securityContextRepository(contexts))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens))
                .addFilterAfter(new SessionValidityFilter(credentials), org.springframework.security.web.context.SecurityContextHolderFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/survey/manual", "/survey/manual/delete", "/admin/survey-members/*/delete",
                                "/admin/survey-members/*/restore", "/admin/survey-members/*/code", "/players/*/restore", "/gear/delete").hasRole("ADMIN")
                        .requestMatchers("/survey/**", "/api/survey/**", "/news/members").hasAnyRole("ADMIN", "EDITOR", "MEMBER")
                        .requestMatchers("/admin/schedules/**", "/admin/survey-members/**",
                                "/players/add", "/players/*/edit", "/players/*/delete",
                                "/fee/**", "/gear/**").hasAnyRole("ADMIN", "EDITOR")
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
