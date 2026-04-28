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
        if (!canManage(session)) return "error/404";

        model.addAttribute("gears", gearService.findAll());
        model.addAttribute("players", playerService.findAll());
        model.addAttribute("userSession", session.getAttribute("userSession"));

        return "gear";
    }

    @PostMapping("/gear/save")
    public String save(@ModelAttribute Gear gear, HttpSession session) {
        if (!canManage(session)) return "error/404";

        gearService.save(gear);
        return "redirect:/gear";
    }

    @PostMapping("/gear/delete")
    public String delete(@RequestParam Long id, HttpSession session) {
        if (!isAdmin(session)) return "error/404";

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