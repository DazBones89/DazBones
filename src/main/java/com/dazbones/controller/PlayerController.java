package com.dazbones.controller;

import com.dazbones.form.PlayerForm;
import com.dazbones.model.Player;
import com.dazbones.model.PlayerPosition;
import com.dazbones.model.UserSession;
import com.dazbones.service.PlayerService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@Controller
public class PlayerController {

    private final PlayerService service;

    private final com.dazbones.service.ImageStorageService images;

    public PlayerController(PlayerService service, com.dazbones.service.ImageStorageService images) {
        this.service = service;
        this.images = images;
    }

    @GetMapping({"/players", "/player"})
    public String list(@RequestParam(value = "sort", required = false, defaultValue = "backNumber") String sort,
                       HttpSession session,
                       Model model) {

        UserSession user = (UserSession) session.getAttribute("userSession");

        List<Player> players = (user != null && user.isAdmin())
                ? service.getAll()
                : service.getActivePlayers();

        if (!List.of("backNumber", "position", "average").contains(sort)) sort = "backNumber";
        players = new ArrayList<>(players);
        java.util.Comparator<Player> byNumber = java.util.Comparator.comparing(Player::getBackNumber,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
        java.util.Comparator<Player> ordering = switch (sort) {
            case "average" -> java.util.Comparator.comparing(Player::getBattingAverage,
                    java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()));
            case "position" -> java.util.Comparator.comparingInt(p -> p.getPositions().stream()
                    .mapToInt(pos -> switch (pos.getPosition()) {
                        case "投手" -> 0;
                        case "捕手" -> 1;
                        case "内野手" -> 2;
                        case "外野手" -> 3;
                        default -> 4;
                    }).min().orElse(5));
            default -> byNumber;
        };
        players.sort(ordering.thenComparing(byNumber).thenComparing(Player::getId));

        model.addAttribute("players", players);
        model.addAttribute("userSession", user);
        model.addAttribute("sort", sort);

        return "player";
    }

    @GetMapping("/players/add")
    public String addPage(HttpSession session, Model model) {
        if (!canManage(session)) {
            return "error/404";
        }

        model.addAttribute("playerForm", new PlayerForm());
        model.addAttribute("userSession", session.getAttribute("userSession"));

        return "playerAdd";
    }

    @PostMapping("/players/add")
    public String add(@Valid @ModelAttribute("playerForm") PlayerForm form,
                      BindingResult result,
                      @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                      HttpSession session,
                      Model model,
                      RedirectAttributes redirectAttributes) {

        if (!canManage(session)) {
            return "error/404";
        }

        if (result.hasErrors()) {
            model.addAttribute("userSession", session.getAttribute("userSession"));
            return "playerAdd";
        }

        Player player = new Player();
        applyFormToPlayer(player, form);

        String uploaded = null;
        try {
            uploaded = images.savePlayer(imageFile);
            if (uploaded != null) player.setImagePath(uploaded);
            service.save(player);
        } catch (Exception e) {
            images.discard(uploaded);
            model.addAttribute("errorMessage", e instanceof IllegalArgumentException ? e.getMessage() : "保存に失敗しました。画像を選び直して再試行してください。");
            model.addAttribute("userSession", session.getAttribute("userSession"));
            return "playerAdd";
        }
        redirectAttributes.addFlashAttribute("successMessage", "選手を登録しました");

        return "redirect:/players";
    }

    @GetMapping("/players/{id}/edit")
    public String editPage(@PathVariable Long id,
                           HttpSession session,
                           Model model) {

        if (!canManage(session)) {
            return "error/404";
        }

        Player player = service.findById(id);
        if (player == null) {
            return "error/404";
        }

        PlayerForm form = toForm(player);

        model.addAttribute("player", player);
        model.addAttribute("playerForm", form);
        model.addAttribute("userSession", session.getAttribute("userSession"));

        return "playerEdit";
    }

    @PostMapping("/players/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("playerForm") PlayerForm form,
                         BindingResult result,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                         HttpSession session,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (!canManage(session)) {
            return "error/404";
        }

        Player player = service.findById(id);
        if (player == null) {
            return "error/404";
        }

        if (result.hasErrors()) {
            model.addAttribute("player", player);
            model.addAttribute("userSession", session.getAttribute("userSession"));
            return "playerEdit";
        }

        String oldImage = player.getImagePath();
        String uploaded = null;
        try {
            uploaded = images.savePlayer(imageFile);
            applyFormToPlayer(player, form);
            if (uploaded != null) player.setImagePath(uploaded);
            service.save(player);
        } catch (Exception e) {
            images.discard(uploaded);
            player.setImagePath(oldImage);
            model.addAttribute("player", player);
            model.addAttribute("userSession", session.getAttribute("userSession"));
            model.addAttribute("errorMessage", e instanceof IllegalArgumentException ? e.getMessage() : "保存に失敗しました。画像を選び直して再試行してください。");
            return "playerEdit";
        }
        if (uploaded != null) images.discard(oldImage);
        redirectAttributes.addFlashAttribute("successMessage", "選手を更新しました");

        return "redirect:/players";
    }

    @PostMapping("/players/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session) {
        if (!canManage(session)) {
            return "error/404";
        }

        service.delete(id);

        return "redirect:/players";
    }

    @PostMapping("/players/{id}/restore")
    public String restore(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return "error/404";
        }

        service.restore(id);

        return "redirect:/players";
    }

    private PlayerForm toForm(Player player) {
        PlayerForm form = new PlayerForm();

        form.setName(player.getName());
        form.setBackNumber(player.getBackNumber());
        form.setThrowHand(player.getThrowHand());
        form.setBatHand(player.getBatHand());
        form.setAtBats(player.getAtBats());
        form.setHits(player.getHits());
        form.setComment(player.getComment());

        List<String> positions = new ArrayList<>();
        if (player.getPositions() != null) {
            for (PlayerPosition position : player.getPositions()) {
                positions.add(position.getPosition());
            }
        }
        form.setPositions(positions);

        return form;
    }

    private void applyFormToPlayer(Player player, PlayerForm form) {
        player.setName(form.getName());
        player.setBackNumber(form.getBackNumber());
        player.setThrowHand(form.getThrowHand());
        player.setBatHand(form.getBatHand());
        player.setAtBats(form.getAtBats() == null ? 0 : form.getAtBats());
        player.setHits(form.getHits() == null ? 0 : form.getHits());
        player.setComment(form.getComment());

        if (player.getPositions() == null) {
            player.setPositions(new ArrayList<>());
        }

        player.getPositions().clear();

        if (form.getPositions() != null) {
            for (String positionName : form.getPositions()) {
                if (positionName == null || positionName.isBlank()) {
                    continue;
                }

                PlayerPosition position = new PlayerPosition();
                position.setPlayer(player);
                position.setPosition(positionName);
                player.getPositions().add(position);
            }
        }
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
