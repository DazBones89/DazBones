package com.dazbones.controller;

import com.dazbones.form.PlayerForm;
import com.dazbones.model.Player;
import com.dazbones.model.PlayerPosition;
import com.dazbones.model.UserSession;
import com.dazbones.repository.PlayerRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
public class PlayerController {

    private final PlayerRepository playerRepository;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public PlayerController(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @GetMapping({"/players", "/player"})
    public String playerList(@RequestParam(value = "sort", required = false, defaultValue = "backNumber") String sort,
                             HttpSession session,
                             Model model) {

        UserSession user = getUserSession(session);

        List<Player> players;
        if (user != null && user.isAdmin()) {
            players = playerRepository.findAll();
        } else {
            players = playerRepository.findActivePlayers();
        }

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
        model.addAttribute("userSession", getUserSession(session));
        return "playerAdd";
    }

    @PostMapping("/players/add")
    public String addPlayer(@ModelAttribute PlayerForm playerForm,
                            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {

        if (!canManage(session)) {
            return "error/404";
        }

        Player player = new Player();
        applyFormToPlayer(player, playerForm);

        player.setDeleteFlg(0);
        player.setCreatedAt(LocalDateTime.now());
        player.setUpdatedAt(LocalDateTime.now());

        Player saved = playerRepository.save(player);

        String imagePath = savePlayerImageIfExists(imageFile, saved.getId(), redirectAttributes);
        if (imagePath != null) {
            saved.setImagePath(imagePath);
            saved.setUpdatedAt(LocalDateTime.now());
            playerRepository.save(saved);
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

        Player player = playerRepository.findById(id).orElse(null);
        if (player == null) {
            return "error/404";
        }

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

        model.addAttribute("player", player);
        model.addAttribute("playerForm", form);
        model.addAttribute("userSession", getUserSession(session));

        return "playerEdit";
    }

    @PostMapping("/players/{id}/edit")
    public String updatePlayer(@PathVariable Long id,
                               @ModelAttribute PlayerForm playerForm,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {

        if (!canManage(session)) {
            return "error/404";
        }

        Player player = playerRepository.findById(id).orElse(null);
        if (player == null) {
            return "error/404";
        }

        applyFormToPlayer(player, playerForm);

        String imagePath = savePlayerImageIfExists(imageFile, player.getId(), redirectAttributes);
        if (imagePath != null) {
            player.setImagePath(imagePath);
        }

        player.setUpdatedAt(LocalDateTime.now());
        playerRepository.save(player);

        return "redirect:/players";
    }

    @PostMapping("/players/{id}/delete")
    public String deletePlayer(@PathVariable Long id,
                               HttpSession session) {

        if (!canManage(session)) {
            return "error/404";
        }

        Player player = playerRepository.findById(id).orElse(null);
        if (player == null) {
            return "error/404";
        }

        player.setDeleteFlg(1);
        player.setDeletedAt(LocalDateTime.now());
        player.setUpdatedAt(LocalDateTime.now());

        playerRepository.save(player);

        return "redirect:/players";
    }

    @PostMapping("/players/{id}/restore")
    public String restorePlayer(@PathVariable Long id,
                                HttpSession session) {

        if (!isAdmin(session)) {
            return "error/404";
        }

        Player player = playerRepository.findById(id).orElse(null);
        if (player == null) {
            return "error/404";
        }

        player.setDeleteFlg(0);
        player.setDeletedAt(null);
        player.setUpdatedAt(LocalDateTime.now());

        playerRepository.save(player);

        return "redirect:/players";
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

    private UserSession getUserSession(HttpSession session) {
        return (UserSession) session.getAttribute("userSession");
    }

    private boolean isAdmin(HttpSession session) {
        UserSession user = getUserSession(session);
        return user != null && user.isAdmin();
    }

    private boolean canManage(HttpSession session) {
        UserSession user = getUserSession(session);
        return user != null && user.canManage();
    }
}