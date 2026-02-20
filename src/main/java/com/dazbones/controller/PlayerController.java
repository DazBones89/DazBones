package com.dazbones.controller;

import com.dazbones.model.Player;
import com.dazbones.model.PlayerForm;
import com.dazbones.service.PlayerService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    // 追加処理（バリデーション付き）
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

        // ★photoPath は今回扱わない（画像アップロードは別機能で）
        // p.setPhotoPath(...);  ←削除

        playerService.save(p);

        redirectAttributes.addFlashAttribute("successMessage", "選手を登録しました");
        return "redirect:/player";
    }

    // （もし編集機能を作りかけなら）編集画面：とりあえず表示だけ
    @GetMapping("/player/edit/{id}")
    public String editPage(@PathVariable Integer id, HttpSession session, Model model) {
        model.addAttribute("session", session);

        Player player = playerService.findById(id);
        if (player == null) {
            return "error/404";
        }

        PlayerForm form = new PlayerForm();
        form.setName(player.getName());
        form.setPosition(player.getPosition());
        form.setNumber(player.getNumber());
        form.setComment(player.getComment());

        // ★id/photoPath は PlayerForm に無いので触らない
        // form.setId(...);        ←削除
        // form.setPhotoPath(...); ←削除

        model.addAttribute("playerForm", form);
        model.addAttribute("playerId", id); // hiddenに入れたい場合用
        return "playerEdit"; // まだ無ければ後で作成
    }

    // （もし編集更新を作りかけなら）更新処理：とりあえず保存まで
    @PostMapping("/player/edit/{id}")
    public String updatePlayer(@PathVariable Integer id,
                               HttpSession session,
                               @Valid PlayerForm playerForm,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        model.addAttribute("session", session);

        if (bindingResult.hasErrors()) {
            model.addAttribute("playerForm", playerForm);
            model.addAttribute("playerId", id);
            return "playerEdit";
        }

        Player player = playerService.findById(id);
        if (player == null) {
            return "error/404";
        }

        player.setName(playerForm.getName());
        player.setPosition(playerForm.getPosition());
        player.setNumber(playerForm.getNumber());
        player.setComment(playerForm.getComment());

        // ★photoPath は今回扱わない
        // player.setPhotoPath(...); ←削除

        playerService.save(player);

        redirectAttributes.addFlashAttribute("successMessage", "選手情報を更新しました");
        return "redirect:/player";
    }
}