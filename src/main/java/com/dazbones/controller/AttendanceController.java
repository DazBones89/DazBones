package com.dazbones.controller;
@org.springframework.stereotype.Controller
public class AttendanceController {
 @org.springframework.web.bind.annotation.GetMapping("/survey/attendance") public String page(){return "redirect:/input?tab=attendance";}
}
