package com.dazbones.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
@RestController
public class HealthController {
    private final JdbcTemplate jdbc;
    public HealthController(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @GetMapping("/health/readiness")
    public ResponseEntity<java.util.Map<String,String>> readiness(){
        try{jdbc.queryForObject("SELECT 1",Integer.class);return ResponseEntity.ok(java.util.Map.of("status","UP"));}
        catch(Exception e){return ResponseEntity.status(503).body(java.util.Map.of("status","DOWN"));}
    }
}
