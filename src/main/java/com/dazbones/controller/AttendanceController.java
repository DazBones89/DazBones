package com.dazbones.controller;

import com.dazbones.model.*;
import com.dazbones.repository.*;
import com.dazbones.service.AttendanceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.*;
import java.util.*;

@Controller
public class AttendanceController {
    private final AttendanceService service;
    private final PlayerRepository players;
    private final SurveyMemberRepository members;
    public AttendanceController(AttendanceService service, PlayerRepository players, SurveyMemberRepository members) {
        this.service = service; this.players = players; this.members = members;
    }

    @GetMapping("/survey/attendance")
    public String page(@RequestParam(required = false) String month, @RequestParam(required = false) Long playerId,
                       HttpSession session, Model model) {
        UserSession user = (UserSession) session.getAttribute("userSession");
        YearMonth selectedMonth = service.month(month);
        List<Player> roster = players.findActivePlayers();
        Long selectedPlayer = user.isAdmin() ? playerId : service.ownPlayer(user);
        if (selectedPlayer == null && user.isAdmin() && !roster.isEmpty()) selectedPlayer = roster.get(0).getId();
        Long target = selectedPlayer;
        boolean editable = target != null && roster.stream().anyMatch(p -> p.getId().equals(target));
        Map<LocalDate, AttendanceAnswer> selectedAnswers = new HashMap<>();
        Map<LocalDate, Map<Long, AttendanceAnswer>> allAnswers = new HashMap<>();
        for (AttendanceAnswer answer : service.answers(selectedMonth)) {
            allAnswers.computeIfAbsent(answer.getTargetDate(), key -> new HashMap<>()).put(answer.getPlayerId(), answer);
            if (answer.getPlayerId().equals(target)) selectedAnswers.put(answer.getTargetDate(), answer);
        }
        model.addAttribute("userSession", user);
        model.addAttribute("month", selectedMonth.toString());
        model.addAttribute("previousMonth", selectedMonth.getYear() == 1900 && selectedMonth.getMonthValue() == 1 ? null : selectedMonth.minusMonths(1).toString());
        model.addAttribute("nextMonth", selectedMonth.getYear() == 2100 && selectedMonth.getMonthValue() == 12 ? null : selectedMonth.plusMonths(1).toString());
        model.addAttribute("dates", service.dates(selectedMonth));
        model.addAttribute("players", roster);
        model.addAttribute("selectedPlayer", target);
        model.addAttribute("editable", editable);
        model.addAttribute("selectedAnswers", selectedAnswers);
        model.addAttribute("allAnswers", allAnswers);
        if (user.isAdmin()) model.addAttribute("members", members.findAll());
        return "attendance";
    }

    @PostMapping("/survey/attendance/answer")
    public String save(@RequestParam Long playerId, @RequestParam String date, @RequestParam String status,
                       @RequestParam(defaultValue = "") String memo, @RequestParam Long version,
                       HttpSession session, RedirectAttributes flash) {
        LocalDate targetDate;
        try { targetDate = LocalDate.parse(date); }
        catch (java.time.format.DateTimeParseException e) { throw new IllegalArgumentException("日付を確認してください"); }
        service.month(YearMonth.from(targetDate).toString());
        try {
            service.save((UserSession) session.getAttribute("userSession"), playerId, targetDate, status, memo, version);
            flash.addFlashAttribute("successMessage", "回答を保存しました");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            flash.addFlashAttribute("draftDate", targetDate);
            flash.addFlashAttribute("draftStatus", status);
            flash.addFlashAttribute("draftMemo", memo);
            flash.addFlashAttribute("draftVersion", version);
        }
        return "redirect:/survey/attendance?month=" + YearMonth.from(targetDate) + "&playerId=" + playerId + "#day-" + date;
    }

    @PostMapping("/admin/attendance/bind")
    public String bind(@RequestParam Long memberId, @RequestParam(required = false) Long playerId,
                       HttpSession session, RedirectAttributes flash) {
        try {
            service.bind((UserSession) session.getAttribute("userSession"), memberId, playerId);
            flash.addFlashAttribute("successMessage", "本人ログインと選手の紐付けを更新しました");
        } catch (IllegalArgumentException e) { flash.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/survey/attendance";
    }
}
