package com.dazbones.controller;
import com.dazbones.model.UserSession;
import com.dazbones.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
@Controller
public class InstagramController {
 private final InstagramService instagram; private final InstagramMetadataClient metadata;
 public InstagramController(InstagramService instagram,InstagramMetadataClient metadata){this.instagram=instagram;this.metadata=metadata;}
 private UserSession user(HttpSession s){return (UserSession)s.getAttribute("userSession");}
 @GetMapping("/admin/instagram") public String page(Model m,HttpSession s){m.addAttribute("posts",instagram.ordered());m.addAttribute("userSession",user(s));return "instagramManage";}
 @PostMapping("/admin/instagram/add") public String add(@RequestParam String url,HttpSession s,RedirectAttributes flash){
  try{var normalized=InstagramMetadataClient.normalize(url);var date=metadata.publishedAt(normalized);instagram.add(user(s),normalized,date);flash.addFlashAttribute("instagramMessage","投稿を登録しました。ホームと写真ページに表示されます。");}
  catch(IllegalArgumentException e){flash.addFlashAttribute("instagramError",e.getMessage());flash.addFlashAttribute("submittedUrl",url);}return "redirect:/admin/instagram";
 }
 @PostMapping("/admin/instagram/delete") public String delete(@RequestParam(required=false) List<Long> ids,HttpSession s,RedirectAttributes flash){try{instagram.delete(user(s),ids);flash.addFlashAttribute("instagramMessage","選択した投稿をサイトから削除しました。Instagramの元投稿は削除していません。");}catch(IllegalArgumentException e){flash.addFlashAttribute("instagramError",e.getMessage());}return "redirect:/admin/instagram";}
 @PostMapping("/admin/instagram/order") public String order(@RequestParam(required=false) Long id,@RequestParam(defaultValue="0") int direction,@RequestParam String revision,@RequestParam(defaultValue="false") boolean reset,@RequestParam(defaultValue="0") int page,HttpSession s,RedirectAttributes flash){
  try{if(reset)instagram.reset(user(s),revision);else instagram.move(user(s),id,direction,revision);flash.addFlashAttribute("instagramMessage",reset?"投稿順に戻しました。":"並び順を更新しました。");}catch(IllegalArgumentException e){flash.addFlashAttribute("instagramError",e.getMessage());}
  return "redirect:/?instagramPage="+Math.max(0,page)+"#instagram";
 }
}
