package com.dazbones.controller;
import com.dazbones.repository.AuditEntryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.*;
@Controller
public class AuditController {
    private final AuditEntryRepository repository;
    public AuditController(AuditEntryRepository repository){this.repository=repository;}
    @GetMapping("/admin/audit")
    public String list(@RequestParam(defaultValue="0") int page,Model model,jakarta.servlet.http.HttpSession session){
        model.addAttribute("userSession",session.getAttribute("userSession"));
        if(page<0 || page>100000)throw new IllegalArgumentException("ページ番号を確認してください");
        model.addAttribute("entries",repository.findAll(PageRequest.of(page,50,Sort.by(Sort.Direction.DESC,"id"))));
        return "admin/audit";
    }
}
