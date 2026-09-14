package com.dazbones.controller;

import com.dazbones.model.Gear;
import com.dazbones.model.UserSession;
import com.dazbones.service.GearService;
import com.dazbones.service.PlayerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class GearController {

    private final GearService gearService;
    private final PlayerService playerService;

    public GearController(GearService gearService, PlayerService playerService) {
        this.gearService = gearService;
        this.playerService = playerService;
    }

    @GetMapping("/gear")
    public String list(HttpSession session, Model model) {
        if (!canManage(session)) {
            return "error/404";
        }

        model.addAttribute("gears", gearService.findAll());
        model.addAttribute("players", playerService.getAll());
        model.addAttribute("userSession", session.getAttribute("userSession"));

        return "gear";
    }

    @PostMapping("/gear/save")
    public String save(@jakarta.validation.Valid @ModelAttribute Gear gear, org.springframework.validation.BindingResult errors,
                       HttpSession session, Model model, org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        if (!canManage(session)) {
            return "error/404";
        }

        if (!errors.hasErrors()) {
            try { gearService.save(gear); }
            catch (IllegalArgumentException e) { errors.reject("invalid", e.getMessage()); }
        }
        if (errors.hasErrors()) {
            model.addAttribute("errorMessage", errors.getAllErrors().get(0).getDefaultMessage());
            var gears = new java.util.ArrayList<>(gearService.findAll());
            gears.removeIf(g -> java.util.Objects.equals(g.getId(), gear.getId()));
            if (gear.getId() != null) gears.add(0, gear);
            else model.addAttribute("draft", gear);
            model.addAttribute("gears", gears);
            model.addAttribute("players", playerService.getAll());
            model.addAttribute("userSession", session.getAttribute("userSession"));
            return "gear";
        }
        flash.addFlashAttribute("successMessage", "道具を保存しました");
        return "redirect:/gear";
    }

    @PostMapping("/gear/delete")
    public String delete(@RequestParam Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return "error/404";
        }

        gearService.delete(id);
        return "redirect:/gear";
    }

    private boolean canManage(HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("userSession");
        return user != null && user.canManage();
    }

    private boolean isAdmin(HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("userSession");
        return user != null && user.isAdmin();
    }
}
