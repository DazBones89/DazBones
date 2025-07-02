package com.dazbones.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SystemInfoController {

    @GetMapping("/system-info")
    public String showSystemInfoPage() {
        return "systemInfo";
    }
}