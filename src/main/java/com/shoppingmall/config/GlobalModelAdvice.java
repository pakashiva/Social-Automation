package com.shoppingmall.config;

import com.shoppingmall.entity.AppSetting;
import com.shoppingmall.repository.AppSettingRepository;
import com.shoppingmall.repository.NotificationRepository;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.web.PageLinks;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    private final AppSettingRepository settings;
    private final NotificationRepository notifications;
    private final PageLinks pageLinks;

    public GlobalModelAdvice(AppSettingRepository settings, NotificationRepository notifications, PageLinks pageLinks) {
        this.settings = settings;
        this.notifications = notifications;
        this.pageLinks = pageLinks;
    }

    @ModelAttribute
    public void addMall(Model model) {
        AppSetting setting = settings.findById(1L).orElseThrow();
        model.addAttribute("mallName", setting.getMallName());
        model.addAttribute("supportEmail", setting.getSupportEmail());
        model.addAttribute("supportPhone", setting.getSupportPhone());
        model.addAttribute("pageLinks", pageLinks);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof MallUserDetails details) {
            model.addAttribute("currentUserName", details.getFullName());
            model.addAttribute("currentRole", details.getRole().name());
            model.addAttribute("unreadCount", notifications.countByUserIdAndReadAtIsNull(details.getId()));
        } else {
            model.addAttribute("unreadCount", 0L);
        }
    }
}
