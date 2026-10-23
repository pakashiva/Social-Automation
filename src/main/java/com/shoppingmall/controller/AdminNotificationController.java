package com.shoppingmall.controller;

import com.shoppingmall.dto.NotificationSendForm;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminNotificationController {

    private final NotificationService notifications;

    public AdminNotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping("/admin/notifications")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("notifications", notifications.listAll(page));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new NotificationSendForm());
        }
        return "admin/notifications";
    }

    @PostMapping("/admin/notifications")
    public String send(
            Authentication authentication,
            @Valid @ModelAttribute("form") NotificationSendForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            model.addAttribute("notifications", notifications.listAll(0));
            return "admin/notifications";
        }
        notifications.broadcast(MallUserDetails.requireId(authentication), form);
        redirectAttributes.addFlashAttribute("success", "Notification sent.");
        return "redirect:/admin/notifications";
    }

    @PostMapping("/admin/notifications/{id}/read")
    public String read(Authentication authentication, @PathVariable long id) {
        notifications.markRead(MallUserDetails.requireId(authentication), id);
        return "redirect:/admin/notifications";
    }
}
