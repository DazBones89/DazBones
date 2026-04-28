package com.dazbones.controller;

import com.dazbones.model.UserSession;
import com.dazbones.service.HolidayService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class HolidayController {

    private final HolidayService service;

    public HolidayController(HolidayService service) {
        this.service = service;
    }

    @GetMapping("/admin/holidays")
    public String list(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "error/404";
        }

        model.addAttribute("holidays", service.getAll());
        model.addAttribute("userSession", session.getAttribute("userSession"));

        return "admin/holidayList";
    }

    @PostMapping("/admin/holidays")
    public String add(@RequestParam("holidayDate") LocalDate holidayDate,
                      @RequestParam("name") String name,
                      HttpSession session,
                      RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return "error/404";
        }

        service.addHoliday(holidayDate, name);
        redirectAttributes.addFlashAttribute("successMessage", "祝日を登録しました");

        return "redirect:/admin/holidays";
    }

    @PostMapping("/admin/holidays/import")
    public String importCsv(@RequestParam("file") MultipartFile file,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return "error/404";
        }

        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "CSVファイルを選択してください");
            return "redirect:/admin/holidays";
        }

        try {
            int count = service.importCsv(file);
            redirectAttributes.addFlashAttribute("successMessage", count + "件の祝日を取り込みました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "CSV取込に失敗しました: " + e.getMessage());
        }

        return "redirect:/admin/holidays";
    }

    @PostMapping("/admin/holidays/{id}/delete")
    public String delete(@PathVariable Long id,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return "error/404";
        }

        service.deleteHoliday(id);
        redirectAttributes.addFlashAttribute("successMessage", "祝日を削除しました");

        return "redirect:/admin/holidays";
    }

    private boolean isAdmin(HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("userSession");
        return user != null && user.isAdmin();
    }
}