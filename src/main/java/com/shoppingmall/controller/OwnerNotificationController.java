package com.shoppingmall.controller;

import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OwnerNotificationController {

    private final NotificationService notifications;

    public OwnerNotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping("/owner/notifications")
    public String list(Authentication authentication, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("notifications", notifications.listMine(MallUserDetails.requireId(authentication), page));
        return "owner/notifications";
    }

    @PostMapping("/owner/notifications/{id}/read")
    public String read(Authentication authentication, @PathVariable long id) {
        notifications.markRead(MallUserDetails.requireId(authentication), id);
        return "redirect:/owner/notifications";
    }
}
