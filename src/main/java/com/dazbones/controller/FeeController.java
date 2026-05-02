package com.dazbones.controller;

import com.dazbones.model.Fee;
import com.dazbones.model.Player;
import com.dazbones.model.UserSession;
import com.dazbones.service.FeeService;
import com.dazbones.service.PlayerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class FeeController {

    private final FeeService feeService;
    private final PlayerService playerService;

    public FeeController(FeeService feeService,
                         PlayerService playerService) {
        this.feeService = feeService;
        this.playerService = playerService;
    }

    @GetMapping("/fee")
    public String list(HttpSession session, Model model) {
        if (!canManage(session)) {
            return "error/404";
        }

        feeService.createMissingFeeRowsForActivePlayers();

        List<Player> players = playerService.getActivePlayers();

        Map<Long, Fee> feeMap = new HashMap<>();
        for (Fee fee : feeService.findAll()) {
            feeMap.put(fee.getPlayerId(), fee);
        }

        long paidCount = 0;
        long unpaidCount = 0;
        int totalAmount = 0;
        int paidAmount = 0;
        int unpaidAmount = 0;

        for (Player player : players) {
            Fee fee = feeMap.get(player.getId());

            int amount = fee != null && fee.getAmount() != null ? fee.getAmount() : 0;
            totalAmount += amount;

            if (fee != null && fee.getPaidFlg() != null && fee.getPaidFlg() == 1) {
                paidCount++;
                paidAmount += amount;
            } else {
                unpaidCount++;
                unpaidAmount += amount;
            }
        }

        model.addAttribute("players", players);
        model.addAttribute("feeMap", feeMap);
        model.addAttribute("paidCount", paidCount);
        model.addAttribute("unpaidCount", unpaidCount);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("paidAmount", paidAmount);
        model.addAttribute("unpaidAmount", unpaidAmount);
        model.addAttribute("userSession", session.getAttribute("userSession"));

        return "fee";
    }

    @PostMapping("/fee/update")
    public String update(@RequestParam Long playerId,
                         @RequestParam(required = false) Integer paidFlg,
                         @RequestParam(required = false) Integer amount,
                         @RequestParam(required = false) String comment,
                         HttpSession session) {

        if (!canManage(session)) {
            return "error/404";
        }

        feeService.saveOrUpdateByPlayer(playerId, paidFlg, amount, comment);

        return "redirect:/fee";
    }

    private boolean canManage(HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("userSession");
        return user != null && user.canManage();
    }
}