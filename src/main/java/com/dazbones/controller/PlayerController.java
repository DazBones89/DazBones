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

    @Value("${app.upload-dir}")
    private String uploadDir;

    public PlayerController(PlayerService service) {
        this.service = service;
    }

    @GetMapping({"/players", "/player"})
    public String list(@RequestParam(value = "sort", required = false, defaultValue = "backNumber") String sort,
                       HttpSession session,
                       Model model) {

        UserSession user = (UserSession) session.getAttribute("userSession");

        List<Player> players = (user != null && user.isAdmin())
                ? service.getAll()
                : service.getActivePlayers();

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

        service.save(player);

        String imagePath = savePlayerImageIfExists(imageFile, player.getId(), redirectAttributes);
        if (imagePath != null) {
            player.setImagePath(imagePath);
            service.save(player);
        }

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

        applyFormToPlayer(player, form);

        String imagePath = savePlayerImageIfExists(imageFile, player.getId(), redirectAttributes);
        if (imagePath != null) {
            player.setImagePath(imagePath);
        }

        service.save(player);

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

    private String savePlayerImageIfExists(MultipartFile file,
                                           Long playerId,
                                           RedirectAttributes redirectAttributes) {

        if (file == null || file.isEmpty()) {
            return null;
        }

        long max = 5L * 1024 * 1024;
        if (file.getSize() > max) {
            redirectAttributes.addFlashAttribute("errorMessage", "画像サイズが5MBを超えています");
            return null;
        }

        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = "";

        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot + 1).toLowerCase();
        }

        if (!(ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("webp"))) {
            redirectAttributes.addFlashAttribute("errorMessage", "対応していない画像形式です");
            return null;
        }

        try {
            Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path playerDir = baseDir.resolve("players");
            Files.createDirectories(playerDir);

            String fileName = "player-" + playerId + "." + ext;
            Path target = playerDir.resolve(fileName);

            try (DirectoryStream<Path> ds = Files.newDirectoryStream(playerDir, "player-" + playerId + ".*")) {
                for (Path p : ds) {
                    Files.deleteIfExists(p);
                }
            }

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/images/players/" + fileName;

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "画像保存に失敗しました: " + e.getMessage());
            return null;
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