package com.dazbones.controller;
@org.springframework.stereotype.Controller
public class GearController {
 @org.springframework.web.bind.annotation.GetMapping("/gear") public String page(){return "redirect:/input?tab=gear";}
}
