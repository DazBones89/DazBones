package com.dazbones.controller;

import com.dazbones.model.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.*;

@Controller
public class AdminMediaController {

    private final com.dazbones.service.ImageStorageService images;
    public AdminMediaController(com.dazbones.service.ImageStorageService images) { this.images = images; }

    @GetMapping("/admin/upload/group-photo")
    public String groupPhotoUploadPage(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "error/404";
        }

        model.addAttribute("userSession", getUserSession(session));
        return "groupPhotoUpload";
    }

    @PostMapping("/admin/upload/group-photo")
    public String uploadGroupPhoto(@RequestParam("file") MultipartFile file,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return "error/404";
        }

        try {
            images.saveGroup(file);
            redirectAttributes.addFlashAttribute("successMessage", "集合写真を更新しました");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "保存に失敗しました。元の画像は保持されています。再試行してください。");
        }
        return "redirect:/admin/upload/group-photo";
    }

    private UserSession getUserSession(HttpSession session) {
        return (UserSession) session.getAttribute("userSession");
    }

    private boolean isAdmin(HttpSession session) {
        UserSession user = getUserSession(session);
        return user != null && user.canManage();
    }
}