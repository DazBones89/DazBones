package com.dazbones.controller;
@org.springframework.stereotype.Controller
public class AnnualFeeController {
 @org.springframework.web.bind.annotation.GetMapping("/fee") public String page(@org.springframework.web.bind.annotation.RequestParam(required=false) Integer year){return "redirect:/input?tab=fee"+(year==null?"":"&year="+year);}
 @org.springframework.web.bind.annotation.GetMapping("/fee/player/{id}") public String history(@org.springframework.web.bind.annotation.PathVariable Long id){return "redirect:/input?tab=fee&playerId="+id;}
}
