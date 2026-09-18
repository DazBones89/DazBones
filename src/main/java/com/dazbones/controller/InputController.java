package com.dazbones.controller;
import com.dazbones.model.*;
import com.dazbones.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;
@Controller
public class InputController {
 private final InputService input;private final AttendanceService attendance;private final PlayerVisibility visibility;
 public InputController(InputService i,AttendanceService a,PlayerVisibility v){input=i;attendance=a;visibility=v;}
 private UserSession user(HttpSession s){return (UserSession)s.getAttribute("userSession");}
 @GetMapping("/input") public String page(HttpSession s,Model m){m.addAttribute("userSession",user(s));return "input";}
 @GetMapping("/api/input") @ResponseBody public Map<String,Object> data(@RequestParam int year,@RequestParam String month,HttpSession s){return input.data(user(s),year,month);}
 @PostMapping("/api/input/stats") @ResponseBody public Map<String,Object> stats(@RequestParam Long playerId,@RequestParam(required=false) Integer atBats,@RequestParam(required=false) Integer hits,@RequestParam Long version,HttpSession s){return input.stats(user(s),playerId,atBats,hits,version);}
 @PostMapping("/api/input/fee") @ResponseBody public Map<String,Object> fee(@RequestParam Long playerId,@RequestParam int year,@RequestParam boolean paid,@RequestParam(defaultValue="") String comment,@RequestParam Long version,HttpSession s){return input.fee(user(s),playerId,year,paid,comment,version);}
 @PostMapping("/api/input/gear") @ResponseBody public Map<String,Object> gear(@RequestParam(required=false) Long id,@RequestParam(defaultValue="") String name,@RequestParam(required=false) Long ownerId,@RequestParam(defaultValue="") String comment,@RequestParam Long version,@RequestParam(defaultValue="false") boolean delete,HttpSession s){return input.gear(user(s),id,name,ownerId,comment,version,delete);}
 @PostMapping("/api/input/attendance") @ResponseBody public Map<String,Object> answer(@RequestParam Long playerId,@RequestParam LocalDate date,@RequestParam String status,@RequestParam(defaultValue="") String memo,@RequestParam Long version,HttpSession s){var a=attendance.save(user(s),playerId,date,status,memo,version);return Map.of("version",a.getVersion());}
 @PostMapping("/api/input/date") @ResponseBody public Map<String,Object> date(@RequestParam LocalDate date,HttpSession s){input.date(user(s),date);return Map.of("saved",true);}
 @GetMapping("/admin/player-settings") public String settings(HttpSession s,Model m){m.addAttribute("userSession",user(s));m.addAttribute("labels",PlayerVisibility.FIELDS);m.addAttribute("fields",visibility.fields(null));return "playerSettings";}
 @PostMapping("/admin/player-settings") @ResponseBody public Map<String,Object> setting(@RequestParam String field,@RequestParam boolean visible){visibility.set(field,visible);return Map.of("saved",true);}
 @PostMapping("/players/{id}/visibility") @ResponseBody public Map<String,Object> visible(@PathVariable Long id,@RequestParam boolean visible,HttpSession s){input.visible(user(s),id,visible);return Map.of("saved",true);}
}
