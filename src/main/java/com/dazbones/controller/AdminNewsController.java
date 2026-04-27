package com.dazbones.controller;

import com.dazbones.form.NewsForm;
import com.dazbones.model.News;
import com.dazbones.model.UserSession;
import com.dazbones.service.NewsService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AdminNewsController {

    private final NewsService service;

    public AdminNewsController(NewsService service) {
        this.service = service;
    }

    @GetMapping("/admin/news")
    public String list(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "error/404";
        }

        model.addAttribute("newsList", service.getAll());
        model.addAttribute("userSession", session.getAttribute("userSession"));

        return "admin/newsList";
    }

    @GetMapping("/admin/news/new")
    public String createPage(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "error/404";
        }

        model.addAttribute("newsForm", new NewsForm());
        model.addAttribute("userSession", session.getAttribute("userSession"));

        return "admin/newsForm";
    }

    @PostMapping("/admin/news")
    public String create(@Valid @ModelAttribute("newsForm") NewsForm form,
                         BindingResult result,
                         HttpSession session,
                         Model model) {
        if (!isAdmin(session)) {
            return "error/404";
        }

        if (result.hasErrors()) {
            model.addAttribute("userSession", session.getAttribute("userSession"));
            return "admin/newsForm";
        }

        News news = new News();
        news.setTitle(form.getTitle());
        news.setContent(form.getContent());
        news.setPublishedAt(form.getPublishedAt());

        service.save(news);

        return "redirect:/admin/news";
    }

    @GetMapping("/admin/news/{id}/edit")
    public String editPage(@PathVariable Long id,
                           HttpSession session,
                           Model model) {
        if (!isAdmin(session)) {
            return "error/404";
        }

        News news = service.findById(id);
        if (news == null) {
            return "error/404";
        }

        NewsForm form = new NewsForm();
        form.setId(news.getId());
        form.setTitle(news.getTitle());
        form.setContent(news.getContent());
        form.setPublishedAt(news.getPublishedAt());

        model.addAttribute("newsForm", form);
        model.addAttribute("userSession", session.getAttribute("userSession"));

        return "admin/newsForm";
    }

    @PostMapping("/admin/news/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("newsForm") NewsForm form,
                         BindingResult result,
                         HttpSession session,
                         Model model) {
        if (!isAdmin(session)) {
            return "error/404";
        }

        News news = service.findById(id);
        if (news == null) {
            return "error/404";
        }

        if (result.hasErrors()) {
            form.setId(id);
            model.addAttribute("userSession", session.getAttribute("userSession"));
            return "admin/newsForm";
        }

        news.setTitle(form.getTitle());
        news.setContent(form.getContent());
        news.setPublishedAt(form.getPublishedAt());

        service.save(news);

        return "redirect:/admin/news";
    }

    @PostMapping("/admin/news/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return "error/404";
        }

        service.delete(id);

        return "redirect:/admin/news";
    }

    private boolean isAdmin(HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("userSession");
        return user != null && user.isAdmin();
    }
}