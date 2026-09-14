package com.dazbones.controller;
import com.dazbones.model.*;
import com.dazbones.form.AnnualFeeForm;
import com.dazbones.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.util.*;
@Controller
public class AnnualFeeController {
    private final AnnualFeeService service;
    private final PlayerService players;
    public AnnualFeeController(AnnualFeeService service,PlayerService players){this.service=service;this.players=players;}
    @GetMapping("/fee")
    public String list(@RequestParam(required=false) Integer year,HttpSession session,Model model){
        int selected=year==null?LocalDate.now().getYear():year;
        if(selected<1900 || selected>2100) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);
        var rows=service.year(selected); var map=new HashMap<Long,AnnualFee>(); rows.forEach(f->map.put(f.getPlayerId(),f));
        var roster=players.getAll().stream().filter(p->map.containsKey(p.getId()) || Integer.valueOf(0).equals(p.getDeleteFlg())).toList();
        model.addAttribute("players",roster);model.addAttribute("feeMap",map);model.addAttribute("year",selected);
        var years=new TreeSet<Integer>(Comparator.reverseOrder()); years.add(selected);years.addAll(service.years());
        model.addAttribute("years",years);model.addAttribute("totals",service.totals(rows));model.addAttribute("teamTotals",service.team());
        model.addAttribute("userSession",session.getAttribute("userSession"));return "annualFee";
    }
    @PostMapping("/fee/update")
    public String update(@Valid @ModelAttribute("feeForm") AnnualFeeForm form,BindingResult errors,
                         HttpSession session,Model model,RedirectAttributes flash){
        if(!errors.hasErrors()) {
            try{service.save(form);}catch(IllegalArgumentException e){errors.reject("invalid",e.getMessage());}
        }
        if(errors.hasErrors()) {
            model.addAttribute("feeErrors",errors.getAllErrors().stream().map(e->e.getDefaultMessage()).toList());
            model.addAttribute("failedForm",form);
            return list(form.getFiscalYear()!=null && form.getFiscalYear()>=1900 && form.getFiscalYear()<=2100?form.getFiscalYear():null,session,model);
        }
        flash.addFlashAttribute("successMessage","年会費を保存しました");return "redirect:/fee?year="+form.getFiscalYear();
    }
    @GetMapping("/fee/player/{id}")
    public String history(@PathVariable Long id,HttpSession session,Model model){
        var p=players.findById(id);if(p==null)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        var rows=service.player(id);model.addAttribute("player",p);model.addAttribute("rows",rows);model.addAttribute("totals",service.totals(rows));
        model.addAttribute("userSession",session.getAttribute("userSession"));return "feeHistory";
    }
}
