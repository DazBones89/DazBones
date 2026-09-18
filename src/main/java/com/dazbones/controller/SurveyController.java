package com.dazbones.controller;
@org.springframework.stereotype.Controller
public class SurveyController {
 @org.springframework.web.bind.annotation.GetMapping("/survey") public String page(){return "redirect:/survey/attendance";}
}
