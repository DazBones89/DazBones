package com.dazbones.controller;

import com.dazbones.model.Player;
import com.dazbones.model.PlayerForm;
import com.dazbones.service.PlayerService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    // 一覧
    @GetMapping("/player")
    public String playerPage(HttpSession session, Model model) {
        model.addAttribute("session", session);
        model.addAttribute("players", playerService.findAll());
        return "player";
    }

    // 追加画面
    @GetMapping("/player/add")
    public String addPage(HttpSession session, Model model) {
        model.addAttribute("session", session);

        if (!model.containsAttribute("playerForm")) {
            model.addAttribute("playerForm", new PlayerForm());
        }
        return "playerAdd";
    }

    // 追加処理
    @PostMapping("/player/add")
    public String addPlayer(HttpSession session,
                            @Valid PlayerForm playerForm,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes redirectAttributes) {

        model.addAttribute("session", session);

        if (bindingResult.hasErrors()) {
            model.addAttribute("playerForm", playerForm);
            return "playerAdd";
        }

        Player p = new Player();
        p.setName(playerForm.getName());
        p.setPosition(playerForm.getPosition());
        p.setNumber(playerForm.getNumber());
        p.setComment(playerForm.getComment());

        playerService.save(p);

        redirectAttributes.addFlashAttribute("successMessage", "選手を登録しました");
        return "redirect:/player";
    }

    // 編集画面
    @GetMapping("/player/edit/{id}")
    public String editPage(@PathVariable Integer id,
                           HttpSession session,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        String role = (String) session.getAttribute("role");
        if (!"admin".equals(role) && !"editor".equals(role)) {
            return "error/404";
        }

        Player player = playerService.findById(id);
        if (player == null) {
            redirectAttributes.addFlashAttribute("successMessage", "対象の選手が見つかりませんでした");
            return "redirect:/player";
        }

        PlayerForm form = new PlayerForm();
        form.setId(player.getId());
        form.setName(player.getName());
        form.setPosition(player.getPosition());
        form.setNumber(player.getNumber());
        form.setComment(player.getComment());

        model.addAttribute("session", session);
        model.addAttribute("playerForm", form);

        return "playerEdit";
    }

    // 更新処理
    @PostMapping("/player/edit")
    public String updatePlayer(HttpSession session,
                               @Valid PlayerForm playerForm,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        String role = (String) session.getAttribute("role");
        if (!"admin".equals(role) && !"editor".equals(role)) {
            return "error/404";
        }

        model.addAttribute("session", session);

        if (bindingResult.hasErrors()) {
            model.addAttribute("playerForm", playerForm);
            return "playerEdit";
        }

        Player player = playerService.findById(playerForm.getId());
        if (player == null) {
            redirectAttributes.addFlashAttribute("successMessage", "対象の選手が見つかりませんでした");
            return "redirect:/player";
        }

        player.setName(playerForm.getName());
        player.setPosition(playerForm.getPosition());
        player.setNumber(playerForm.getNumber());
        player.setComment(playerForm.getComment());

        playerService.save(player);

        redirectAttributes.addFlashAttribute("successMessage", "選手情報を更新しました");
        return "redirect:/player";
    }

    // 削除
    @PostMapping("/player/delete/{id}")
    public String deletePlayer(@PathVariable Integer id,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {

        String role = (String) session.getAttribute("role");
        if (!"admin".equals(role) && !"editor".equals(role)) {
            return "error/404";
        }

        if (!playerService.existsById(id)) {
            redirectAttributes.addFlashAttribute("successMessage", "対象の選手が見つかりませんでした");
            return "redirect:/player";
        }

        playerService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "選手を削除しました");

        return "redirect:/player";
    }
}