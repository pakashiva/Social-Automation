package com.shoppingmall.controller;

import com.shoppingmall.dto.SettingsForm;
import com.shoppingmall.security.MallUserDetails;
import com.shoppingmall.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminSettingsController {

    private final SettingsService settings;

    public AdminSettingsController(SettingsService settings) {
        this.settings = settings;
    }

    @GetMapping("/admin/settings")
    public String form(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", settings.get());
        }
        return "admin/settings";
    }

    @PostMapping("/admin/settings")
    public String update(
            Authentication authentication,
            @Valid @ModelAttribute("form") SettingsForm form,
            BindingResult binding,
            RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            return "admin/settings";
        }
        settings.update(MallUserDetails.requireId(authentication), form);
        redirectAttributes.addFlashAttribute("success", "Settings saved.");
        return "redirect:/admin/settings";
    }
}
