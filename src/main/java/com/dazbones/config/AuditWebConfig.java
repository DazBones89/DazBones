package com.dazbones.config;

import com.dazbones.model.UserSession;
import com.dazbones.service.AuditService;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.support.RequestContextUtils;

@Configuration
public class AuditWebConfig implements WebMvcConfigurer {
    private final AuditService audit;
    public AuditWebConfig(AuditService audit){this.audit=audit;}
    @Override public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(new HandlerInterceptor(){
            public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler){
                HttpSession session=request.getSession(false);
                if("POST".equals(request.getMethod()) && session!=null && !request.getRequestURI().equals("/login"))
                    request.setAttribute("audit.actor",session.getAttribute("userSession"));
                return true;
            }
            public void postHandle(HttpServletRequest request,HttpServletResponse response,Object handler,ModelAndView view){
                boolean invalid=RequestContextUtils.getOutputFlashMap(request).containsKey("errorMessage");
                if(view!=null) invalid |= view.getModel().values().stream().anyMatch(v->v instanceof BindingResult b && b.hasErrors());
                request.setAttribute("audit.invalid",invalid);
            }
            public void afterCompletion(HttpServletRequest request,HttpServletResponse response,Object handler,Exception exception){
                if(!(request.getAttribute("audit.actor") instanceof UserSession user))return;
                String target=request.getRequestURI();
                // Only validated identifiers: never record codes, names, comments, amounts or request bodies.
                for(String key:java.util.List.of("id","memberId","playerId","date","fiscalYear")){
                    String value=request.getParameter(key);
                    if(value!=null && value.matches("[0-9-]{1,20}"))target+=" "+key+"="+value;
                }
                String outcome=exception!=null || response.getStatus()>=400 ? "失敗" : Boolean.TRUE.equals(request.getAttribute("audit.invalid")) ? "入力エラー" : "完了";
                try{audit.record(user,target.substring(0,Math.min(255,target.length())),outcome,response.getStatus());}
                catch(Exception e){org.slf4j.LoggerFactory.getLogger(AuditWebConfig.class).error("Operation audit could not be saved",e);}
            }
        }).excludePathPatterns("/error");
    }
}
