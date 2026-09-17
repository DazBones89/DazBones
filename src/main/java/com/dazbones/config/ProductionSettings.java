package com.dazbones.config;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
@Configuration @Profile("prod")
public class ProductionSettings {
    public ProductionSettings(@Value("${app.auth.admin-code}") String admin,@Value("${app.auth.editor-code}") String editor,
                              @Value("${spring.datasource.password}") String password){
        for(String value:java.util.List.of(admin,editor,password))
            if(value.length()<16 || value.startsWith("replace-")) throw new IllegalStateException("Production requires new random credentials of at least 16 characters");
        if(admin.equals(editor))throw new IllegalStateException("Admin and editor codes must differ");
    }
}
