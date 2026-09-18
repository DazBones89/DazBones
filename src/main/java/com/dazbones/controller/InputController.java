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
 @GetMapping({"/players/stats", "/fee", "/gear", "/survey/attendance"})
 public String page(jakarta.servlet.http.HttpServletRequest request,HttpSession s,Model m){
  String page=switch(request.getRequestURI()){case "/fee" -> "fee";case "/gear" -> "gear";case "/survey/attendance" -> "attendance";default -> "stats";};
  String title=switch(page){case "fee" -> "部費";case "gear" -> "道具管理";case "attendance" -> "出欠確認";default -> "打撃成績";};
  m.addAttribute("userSession",user(s));m.addAttribute("inputPage",page);m.addAttribute("inputTitle",title);return "input";
 }
 @GetMapping("/input") public String legacy(@RequestParam(defaultValue="attendance") String tab,@RequestParam(required=false) String year,@RequestParam(required=false) String month,@RequestParam(required=false) String playerId){
  String target=switch(tab){case "stats" -> "/players/stats";case "fee" -> "/fee";case "gear" -> "/gear";default -> "/survey/attendance";};
  var url=org.springframework.web.util.UriComponentsBuilder.fromPath(target);
  if(year!=null)url.queryParam("year",year);if(month!=null)url.queryParam("month",month);if(playerId!=null)url.queryParam("playerId",playerId);
  return "redirect:"+url.build().encode().toUriString();
 }
 @GetMapping("/fee/player/{id}") public String feeHistory(@PathVariable Long id){return "redirect:/fee?playerId="+id;}
 @GetMapping("/api/input") @ResponseBody public Map<String,Object> data(@RequestParam int year,@RequestParam String month,HttpSession s){return input.data(user(s),year,month);}
 @PostMapping("/api/input/stats") @ResponseBody public Map<String,Object> stats(@RequestParam Long playerId,@RequestParam(required=false) Integer atBats,@RequestParam(required=false) Integer hits,@RequestParam Long version,HttpSession s){return input.stats(user(s),playerId,atBats,hits,version);}
 @PostMapping("/api/input/fee") @ResponseBody public Map<String,Object> fee(@RequestParam Long playerId,@RequestParam int year,@RequestParam boolean paid,@RequestParam(defaultValue="") String comment,@RequestParam Long version,HttpSession s){return input.fee(user(s),playerId,year,paid,comment,version);}
 @PostMapping("/api/input/gear") @ResponseBody public Map<String,Object> gear(@RequestParam(required=false) Long id,@RequestParam(defaultValue="") String name,@RequestParam(required=false) Long ownerId,@RequestParam(defaultValue="") String comment,@RequestParam Long version,@RequestParam(defaultValue="false") boolean delete,HttpSession s){return input.gear(user(s),id,name,ownerId,comment,version,delete);}
 @PostMapping("/api/input/attendance") @ResponseBody public Map<String,Object> answer(@RequestParam Long playerId,@RequestParam LocalDate date,@RequestParam String status,@RequestParam(defaultValue="") String memo,@RequestParam Long version,HttpSession s){var a=attendance.save(user(s),playerId,date,status,memo,version);return Map.of("version",a.getVersion());}
 @PostMapping("/api/input/date") @ResponseBody public Map<String,Object> date(@RequestParam LocalDate date,HttpSession s){input.date(user(s),date);return Map.of("saved",true);}
 public record BulkAttendance(Long playerId,String month,String status,Map<LocalDate,Long> versions) {}
 @PostMapping("/api/input/attendance/bulk") @ResponseBody
 public Map<String,Object> bulk(@RequestBody BulkAttendance request,HttpSession s){
  if(request.playerId()==null || request.month()==null)throw new IllegalArgumentException("選手と月を指定してください");
  var saved=attendance.saveMonth(user(s),request.playerId(),request.month(),request.status(),request.versions());
  return Map.of("answers",saved.stream().map(a->Map.of("playerId",a.getPlayerId(),"date",a.getTargetDate().toString(),"status",a.getStatus(),"memo",Objects.toString(a.getMemo(),""),"version",a.getVersion())).toList());
 }
 @GetMapping("/admin/player-settings") public String settings(HttpSession s,Model m){m.addAttribute("userSession",user(s));m.addAttribute("labels",PlayerVisibility.FIELDS);m.addAttribute("fields",visibility.fields(null));return "playerSettings";}
 @PostMapping("/admin/player-settings") @ResponseBody public Map<String,Object> setting(@RequestParam String field,@RequestParam boolean visible){visibility.set(field,visible);return Map.of("saved",true);}
 @PostMapping("/players/{id}/visibility") @ResponseBody public Map<String,Object> visible(@PathVariable Long id,@RequestParam boolean visible,HttpSession s){input.visible(user(s),id,visible);return Map.of("saved",true);}
}
