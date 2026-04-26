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

    @Value("${app.upload-dir}")
    private String uploadDir;

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

        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "ファイルが選択されていません");
            return "redirect:/admin/upload/group-photo";
        }

        long max = 5L * 1024 * 1024;
        if (file.getSize() > max) {
            redirectAttributes.addFlashAttribute("errorMessage", "ファイルサイズが5MBを超えています");
            return "redirect:/admin/upload/group-photo";
        }

        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = "";

        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot + 1).toLowerCase();
        }

        if (!(ext.equals("jpg") || ext.equals("jpeg"))) {
            redirectAttributes.addFlashAttribute("errorMessage", "対応していない形式です（JPG/JPEGのみ）");
            return "redirect:/admin/upload/group-photo";
        }

        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            Path target = dir.resolve("group-photo.jpg");

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            redirectAttributes.addFlashAttribute("successMessage", "集合写真を更新しました");
            return "redirect:/admin/upload/group-photo";

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "保存に失敗しました: " + e.getMessage());
            return "redirect:/admin/upload/group-photo";
        }
    }

    private UserSession getUserSession(HttpSession session) {
        return (UserSession) session.getAttribute("userSession");
    }

    private boolean isAdmin(HttpSession session) {
        UserSession user = getUserSession(session);
        return user != null && user.isAdmin();
    }
}