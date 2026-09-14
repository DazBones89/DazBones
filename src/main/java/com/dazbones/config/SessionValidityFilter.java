package com.dazbones.config;
import com.dazbones.model.UserSession;
import com.dazbones.service.CredentialService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class SessionValidityFilter extends OncePerRequestFilter {
    private final CredentialService credentials;
    public SessionValidityFilter(CredentialService credentials){this.credentials=credentials;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        var session=request.getSession(false);
        if(session!=null && session.getAttribute("userSession") instanceof UserSession user && !credentials.isValid(user)) {
            session.invalidate(); SecurityContextHolder.clearContext();
        }
        chain.doFilter(request,response);
    }
}
