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
                        .requestMatchers("/api/survey/**", "/survey/manual/**", "/survey/answer", "/admin/survey-members/**", "/admin/attendance/bind", "/fee/update").denyAll()
                        .requestMatchers("/admin/holidays/**", "/admin/code", "/admin/audit", "/admin/player-settings/**", "/players/*/visibility", "/players/*/delete", "/players/*/restore").hasRole("MASTER")
                        .requestMatchers("/players/stats", "/input/**", "/api/input/**", "/survey/**", "/news/members", "/admin/**", "/players/add", "/players/*/edit", "/fee/**", "/gear/**").hasAnyRole("MASTER", "PLAYER")
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
